# BrainBrawl / Pergunta o Luso - Technical Audit Report

## 1. Executive Summary
The BrainBrawl project is in an advanced state of development, featuring a robust MVVM-style architecture and a well-structured Firebase integration. Most core game flows (Solo, Group, 1v1, 2v2, Matchmaking) are implemented. However, the project faces significant stability issues in the multiplayer podium and potential logic flaws regarding host roles and statistics persistence. 

- **Current State**: Feature-complete for most modes, but requires polish and bug fixing in multiplayer synchronization.
- **Closed Beta Readiness**: **Near Ready**, but the podium instability and host-counting logic are blockers.
- **Biggest Blockers**: Multiplayer podium synchronization, incorrect participant counting, and potential race conditions in matchmaking.
- **Biggest Opportunities**: Refining the competitive experience and ensuring a fair XP/Ranking system.

## 2. Project Score (0-20)
| Category | Score | Notes |
| :--- | :--- | :--- |
| **Code Quality** | 16/20 | Clean Kotlin code, good use of ViewBinding and LiveData. |
| **Architecture** | 15/20 | MVVM is mostly followed, but some Activities are becoming "God Objects". |
| **Stability** | 12/20 | Main issues in multiplayer synchronization and podium logic. |
| **Firebase Design** | 17/20 | Excellent use of `FirebasePaths` and organized nodes. |
| **UI/UX** | 14/20 | Functional but lacks some visual consistency across modes. |
| **Fun/Game Potential** | 18/20 | Strong potential with varied modes (Chaotic, Eliminations). |
| **Closed Beta Readiness**| 14/20 | High, once the podium/host bugs are resolved. |
| **Public Release Ready**| 10/20 | Needs security hardening (Rules) and anti-abuse measures. |
| **AVERAGE SCORE** | **14.5 / 20** | |

## 3. Modules/Files Reviewed
| File/Module | Status | Problems Found | Risk Level | Priority |
| :--- | :--- | :--- | :--- | :--- |
| `SalaRepository` | ⚠️ Warning | Complex player identification logic; potential for collisions. | Medium | High |
| `JogoRepository` | ✅ Good | Solid abstraction of game actions. | Low | Low |
| `MatchmakingRepository`| ⚠️ Warning | Non-atomic stale cleanup; potential race conditions. | Medium | Medium |
| `PontuacaoRepository` | ❌ Critical | **Podium participant counting logic is flawed.** | High | Critical |
| `JogoViewModel` | ⚠️ Warning | Handles too many responsibilities (game logic + navigation). | Medium | Medium |
| `PontuacoesViewModel` | ⚠️ Warning | Synchronization logic for group results is fragile. | High | High |
| `EstatisticasService` | ✅ Good | Pure logic, easily testable. | Low | Low |

## 4. Closed Beta Blockers
- **Multiplayer Podium**: Fix the "waiting for results" hang (e.g., 2/3) caused by counting non-playing hosts.
- **Host Role**: Ensure hosts who don't play are never counted as participants.
- **Matchmaking Ghost Players**: Improve cleanup of the `FILA` node when a player cancels or disconnects abruptly.
- **XP Farming**: Restrict XP gain in very short or "easy" custom categories if possible.

## 5. Important Technical Problems
| Priority | Area | Description | Probable Files | Impact | Recommended Fix |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Critical** | Group Podium | Counting `isHostOnly` or non-active players in `totalJogadores`. | `PontuacaoRepository.kt` | Podium hangs or shows empty slots. | Filter by `isHostOnly == false` and `estado != OFF` in `toResultadosGrupo`. |
| **High** | Persistence | Admin/Host saving 0-point results to "complete" the podium. | `JogoViewModel.kt` | Inflates "Games Played" for hosts who didn't play. | Skip `guardarResultadoJogador` if `admin` and `!playing`. |
| **Medium** | Matchmaking | Non-atomic `limparStale` and `guestComMesmoNomeUpdates`. | `MatchmakingRepository.kt`| Inconsistent queue states. | Move cleanup logic to a Cloud Function or use atomic transactions for everything. |
| **Medium** | UX | Back button blocked message appears even after game ends. | `JogoActivity.kt` | Minor annoyance. | Disable `OnBackPressedCallback` once the game transitions to results. |

## 6. Group Podium Analysis
- **Current Behavior**: The podium listens to the `JOGADORES` node of a room. It calculates `totalJogadores` by counting all children that are not "admin" and not "OFF". It waits until `resultadosGuardados` matches `totalJogadores`.
- **Failure Point**: Human hosts are added to the `JOGADORES` node but might not actually "play" (answering questions). If they don't answer, they don't reach a `TERMINADO` state quickly, or if they stay in the room but don't save results, the count stays at (e.g.) 1/2 players.
- **Admin/Host Behavior**: In `JogoViewModel`, even admins call `guardarResultadoJogador` at the end to "fill" the podium. This is a workaround that causes the host to appear with 0 points.
- **Recommended Change**: 
    1. Introduce a explicit `isPlaying` flag or strictly use `isHostOnly`.
    2. In `PontuacaoRepository.toResultadosGrupo`, filter out anyone where `isHostOnly == true`.
    3. Ensure the Host/Admin only sets `isHostOnly = true` if they are observing.

## 7. Matchmaking Analysis
- **Flow**: Player enters `FILA` -> Host (first player) claims a `MATCH` via transaction -> Host creates the room and writes `RESULTADOS` for all players -> Players move to the room.
- **Risks**:
    - If the "Host" fails to create the room after claiming the match, other players are stuck in `FILA` with `ENCONTRADO` state.
    - `MATCH_CREATION_TIMEOUT_MS` (15s) helps, but players might perceive it as a hang.
- **Ghost Players**: Occur when a player's `onDisconnect` doesn't fire or when `limparStale` fails to catch them. 

## 8. Firebase and Security Analysis
- **Paths**: Consistent and well-mapped.
- **Rules**: High risk of "Client-side writes" to other players' nodes if rules are not strict. For example, a player could theoretically write to the `PONTUACAO` of an opponent in the same room.
- **Guest Restrictions**: Well-handled in code (`podeGravarPersistente`), but must be enforced in Firebase Rules to prevent guests from writing to `ranking` or `historico` nodes.

## 9. Score/Ranking/XP/Badges Analysis
- **Fairness**: XP gain is currently linear. Solo play grants XP, which is fine for retention but might inflate levels compared to competitive skill.
- **Abuse Risk**: Custom categories can be used to farm XP (10 questions, 1 second each). 
- **Recommendation**: Cap XP from Solo/Custom categories per day or reduce XP multiplier for non-official categories.

## 10. UX/Gameplay Analysis
- **Visual Continuity**: Ensure the transition from "Waiting Room" to "Game" and then "Podium" feels seamless.
- **Podium Closure**: If the podium "closes immediately," check if `observarSalaApagada` is triggering too early because the Host left the room before others saw the results.

## 11. Recommended Roadmap
### Must fix before Closed Beta:
1. Fix Podium participant counting (ignore non-playing host).
2. Fix "Waiting for results" hang.
3. Ensure Host-only doesn't get stats.
### Should fix soon:
1. Atomic matchmaking cleanup.
2. XP/Ranking balance.
3. Visual polish of the Podium.

## 12. Testing Checklist
- [ ] **Group Classic**: 3 players, host doesn't play -> Podium should show 2 players.
- [ ] **Group Classic**: 3 players, host plays -> Podium should show 3 players.
- [ ] **Eliminations**: 4 players, 1st eliminated -> Must see "Waiting" screen, then Podium at the end.
- [ ] **Matchmaking**: 2 players join, 1 cancels immediately -> Queue should remain stable.

## 13. Final Conclusion
**Should you continue?** Yes, absolutely. The project is technically sound and has a clear vision.
**Send to friends soon?** Yes, after fixing the Podium/Host logic.
**What to fix first?** `PontuacaoRepository.kt` logic for counting players and `JogoViewModel.kt` for result saving.
