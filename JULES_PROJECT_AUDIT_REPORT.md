# Relatório de Auditoria do Projeto: BrainBrawl / Pergunta o Luso

## 1. Resumo executivo

O projeto **Pergunta o Luso** apresenta uma base sólida para um jogo de quiz multijogador em tempo real utilizando Kotlin, ViewBinding e Firebase. A arquitetura está organizada em ViewModels, Repositories e Services, o que facilita a separação de responsabilidades. No entanto, existem pontos críticos de estabilidade (em particular no Matchmaking e no Pódio de Grupo) e vulnerabilidades de segurança (validação client-side vs server-side) que precisam ser endereçados antes de uma Beta pública. O jogo tem um grande potencial, mas está num estado "Late Alpha / Pre-Beta", sendo aconselhável corrigir bloqueadores técnicos antes de partilhar com utilizadores reais para garantir uma boa primeira impressão.

**Maiores bloqueadores:**
- Lógica de sincronização e concorrência no Firebase (Matchmaking e Pódios).
- Ausência aparente de Firebase Cloud Functions ou Security Rules rigorosas (o cliente faz muita lógica crítica).

**Maiores oportunidades:**
- A base de código está limpa e modular.
- As mecânicas de jogo (1x1, 2x2, Grupos, Custom Categories) já estão implementadas.

---

## 2. Nota geral do projeto

* **Código:** 16/20 (Bem estruturado, mas dependente de validação no cliente)
* **Arquitetura:** 15/20 (Boa separação MVC/MVVM, mas Activities ainda têm responsabilidade de navegação complexa)
* **Estabilidade:** 13/20 (Bugs reportados em pódios e matchmaking devido a concorrência assíncrona)
* **UI/UX:** 14/20 (Fluxos de sala de espera e pódio necessitam de afinação)
* **Diversão:** 17/20 (Múltiplos modos de jogo, sistema de badges/XP e criação de categorias adicionam imenso valor)
* **Pronto para beta fechada:** 14/20 (Pode ser testado com amigos próximos, mas necessita de regras de segurança básicas)

---

## 3. Ficheiros/módulos analisados

| Ficheiro / Módulo | Estado | Problemas | Risco | Prioridade |
|-------------------|--------|-----------|-------|------------|
| `MatchmakingViewModel` / `Repository` | Estável, mas frágil | Jogadores fantasma, cancelamento assíncrono complexo | Alto | Alta |
| `SalaGrupoViewModel` / `PontuacaoRepository` | Buggado | Pódio encerra rápido, contagem 0/3 falha, hosts a contar | Alto | Alta |
| `ScoreService` / `EstatisticasService` | Bom | Lógica de cálculo no cliente permite manipulação (cheat/farm) | Médio | Média |
| `CategoriaRepository` | Bom | Validações base de perguntas/opções são locais | Médio | Média |
| `FirebasePaths` | Excelente | Centralizado, mas algumas strings "admin" e legadas soltas | Baixo | Baixa |

---

## 4. Bloqueadores antes da beta fechada (Amigos)

- **Correção do Pódio do Modo Grupo:** O pódio tem de esperar de forma fiável pelos resultados apenas dos jogadores ativos (excluindo admins/placeholders e hosts inativos). Se fechar sozinho prematuramente, estraga a experiência de vitória.
- **Matchmaking Ghosting:** Garantir que quando um jogador cancela a procura, a sua entrada na fila do Firebase é garantidamente removida (mesmo se a app for fechada abruptamente - usar `onDisconnect` eficazmente).

---

## 5. Problemas técnicos importantes

| Prioridade | Área | Descrição | Ficheiros prováveis | Impacto | Sugestão |
|------------|------|-------------|---------------------|---------|----------|
| Alta | Pódios | Pódio do modo grupo sai logo ou fica "A aguardar... 0/3". Contagem de jogadores reais incorreta. | `PontuacaoRepository.kt` (`toResultadosGrupo`), `PontuacoesViewModel.kt` | Jogadores não vêem quem ganhou, falha na gravação de XP. | Refinar `deveIgnorarJogador`. Garantir que o Host não ativo não é esperado no cálculo `resultadosGuardados >= totalJogadores`. |
| Alta | Matchmaking | Jogadores fantasma e salas cheias indevidas. Cancelamento falha em sincronia. | `MatchmakingRepository.kt`, `MatchmakingViewModel.kt` | Jogadores ficam presos em filas mortas ou em 1v0. | Usar transações Firebase mais rigorosas na criação de salas. Garantir `onDisconnect().removeValue()` nas filas. |
| Média | Segurança | Clientes calculam os próprios pontos e gravam XP/Stats na BD. | `EstatisticasService.kt`, `ScoreService.kt` | Exploits, XP farming (hackers podem forçar updates Firebase). | Mover o cálculo de vitórias, pontuação e XP para Firebase Cloud Functions no futuro. |
| Média | Categorias | Farm de vitórias usando categorias custom/fáceis. | `Pontuacao1x1ViewModel.kt`, `PontuacoesViewModel.kt` | Quebra o valor do Ranking. | Categorias públicas/custom não devem contar para os modos competitivos ou dar XP reduzido. |
| Baixa | Convidados | Convidados podem persistir dados se houver falha de flag `isGuest`. | `JogadorSalaIdentidade.kt`, Repositories | Lixo na base de dados. | Reforçar `podeGravarPersistente()` garantindo que UIDs falsos ou vazios nunca chamam `guardarHistoricoUmaVez`. |

---

## 6. Problemas de UX

| Ecrã | Problema | Efeito no jogador | Sugestão |
|------|----------|--------------------|----------|
| Pódio (Grupo) | Fecha rápido demais se alguém sair ou enviar dados cedo. | Frustração, não dá tempo de ver os resultados finais. | Adicionar um botão "Continuar" obrigatório ou tempo mínimo de exibição (ex: 5s). |
| Sala de Espera | Host que não joga confunde os jogadores na lista. | Não sabem se o jogo vai começar ou contra quem vão jogar. | Separar UI: "Host / Espectador" vs "A Jogar". |
| Matchmaking | Cancelar a fila pode demorar ou não dar feedback imediato. | Sensação de lag, jogadores desistem da app. | UI deve bloquear interações e mostrar spinner de cancelamento fiável. |

---

## 7. Pódio Grupo (Análise Profunda)

* **Como funciona:** O `PontuacoesViewModel` escuta a Firebase (via `escutarResultadosGrupo`). Conta `totalJogadores` reais e aguarda que `resultadosGuardados` iguale o total para declarar `resumo.completos = true`. Se completo, desenha o pódio e grava o histórico.
* **Onde falha:**
  * Host que não joga: Se a tag `isHostOnly` falhar ou for ignorada, a app fica à espera do resultado do Host. O pódio fica em 0/3 ou 2/3 infinitamente.
  * Placeholder/Admin falso: Se o identificador do placeholder não for apanhado por `deveIgnorarJogador`, a app espera pelo placeholder.
  * Fechar sozinho: Se um jogador sair rápido da sala (apagando o seu `node`), o número de `totalJogadores` desce, ativando `completos = true` instantaneamente para os restantes, cortando a experiência.

---

## 8. Matchmaking (Análise Profunda)

* **Fluxo atual:** O utilizador entra em `FirebasePaths.FILA` no seu modo. Um sistema de "transação" (ver `tentarCriarMatch`) pega nos N primeiros jogadores `AGUARDANDO` e gera uma sala, mudando o seu estado para `ENCONTRADO`.
* **Riscos e Bugs:** Concorrência. Se 2 jogadores tentarem criar o match em simultâneo usando Transações no cliente, podem ocorrer sobreposições se a Transaction do Firebase for interrompida ou desatualizada. Jogadores fantasma ocorrem quando a app crasha e o `onDisconnect` não apaga rápido o suficiente, ou o jogador entra numa sala, desiste, mas a sala ainda existe.

---

## 9. Firebase e Segurança

* O facto de a aplicação estar a gravar XP, Vitórias, e Resultados diretamente a partir do código Kotlin do Cliente (`jogadorRef.updateChildren(updates)`) é um **pesadelo de segurança para um jogo competitivo**. Qualquer utilizador com um APK modificado ou usando pedidos REST pode injetar `{ xpTotal: 99999, totalVitorias: 5000 }` no seu perfil.
* **O que bloqueia a beta pública:** Regras rígidas (`.write`) de Firebase Realtime Database. Neste momento, é aceitável para Beta Fechada (com amigos confiáveis). Para Beta Pública, é estritamente obrigatório mover lógicas de cálculo de score e progressão para as **Cloud Functions**.

---

## 10. Pontuação, Ranking, XP e Badges

* **Farm e Abuso:** Modos Solo e Categorias Personalizadas podem ser abusados para ganhar badges e XP infinitos. O código atual verifica `podeGravarPersistente()`, mas deve garantir explicitamente que estatísticas competitivas (Ranking, Recordes, Vitórias) **só são atualizadas em Modos Oficiais e Matchmaking**.
* O emparelhamento de Host inativo ou Empates 2x2 parece corretamente desenhado no `EstatisticasService`, que trata empates não atribuindo vitória a nenhuma equipa, evitando exploits de 2x2.

---

## 11. Funcionalidades Futuras e Road Map

**Melhorias Rápidas (Antes da Beta):**
* Corrigir bug do Pódio (ignorar jogadores que saem sem fechar o pódio prematuramente, corrigir filtragem de Host).
* Adicionar tempo de tolerância no Matchmaking para Ghosts.
* Confirmar que "Categorias Personalizadas" não somam XP competitivo.

**v1.3 (Beta Pública):**
* Migrar lógicas de Pontuação/XP/Ranking para Firebase Cloud Functions.
* Implementar Firebase Security Rules robustas (ex: só o dono pode editar o próprio perfil, e apenas os campos "Nome/Avatar", não os campos de XP).

**v1.4:**
* Tabelas de Liderança Semanais/Mensais (Atualmente o histórico limpa em 3 dias/50 entradas).

**v2.0 (Ideias Grandes para Retenção):**
* Recompensas diárias (Daily Login Bonus).
* Avatares ou Títulos desbloqueáveis via XP/Badges (atualmente avatar é escolhido livremente).

---

## 12. Plano Recomendado

1. **Testes Locais (Corrigir 1st):** Arrumar as lógicas do `deveIgnorarJogador` no Pódio e estabilizar o botão Cancelar no Matchmaking.
2. **Beta Fechada (com Amigos):** Lançar APK via TestFlight/AppDistribution para testar bugs de latência em multijogador.
3. **Migração Cloud Functions:** Reescrever o `ScoreService` e `EstatisticasService` em Node.js (Firebase Functions).
4. **Beta Pública:** Lançar nas lojas.

---

## 13. Checklist de Beta Fechada (Para fazer com amigos)

- [ ] Entrar 3 pessoas numa sala de Grupo, 1 delas como "Host Only". Verificar se o jogo começa e se o pódio espera pelos 2 jogadores reais.
- [ ] Jogador fecha a app a meio do ecrã de Matchmaking. O outro jogador tenta encontrar partida. Verifica se a sala encrava ou encontra um jogador vivo.
- [ ] Jogar 1x1 em categoria "Oficial" -> Confirmar subida de XP.
- [ ] Jogar 1x1 em categoria "Custom" feita pelo host -> Confirmar que NÃO sobe na tabela de Ranking competitivo.
- [ ] Verificar se "Convidados" não ganham espaço na BD do Firebase (confirmar em consola do Firebase se os Guest IDs são apagados ou negados no histórico).

---

## 14. Conclusão

* **Vale continuar?** Absolutamente. A base está muito boa, a UI tem bases fortes e o jogo é mecanicamente sólido.
* **Vale mandar a amigos?** Sim. É o momento exato para uma Beta Fechada para descobrir os edge-cases da latência da rede e do Firebase.
* **Obrigatório corrigir antes?** Bugs críticos do Pódio do Modo Grupo que arruínam a sensação de conclusão.
* **O que pode ficar para depois?** As Firebase Cloud Functions (Anti-Cheat), modos de jogo avançados e refatorizações de UI menores.
