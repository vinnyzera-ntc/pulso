# Pulso — roadmap

App pessoal, como um bloco de notas para anotar lembretes e alarmes, de uso individual (instalação direta, sem Play Store).

## v1

| Fase | Conteúdo | Estado |
|---|---|---|
| 0 | Setup do projeto e tema Pulso | Concluída |
| 1 | Dados (Room), Google Agenda via Calendar Provider, Ajustes e permissões | Concluída |
| 2 | Hoje e Tarefas | Concluída |
| 3 | Agenda (dia, semana, mês) e compromissos | Concluída |
| 4 | Captura rápida (parser pt-BR local) e Modo foco | Concluída |
| 5 | Lembretes de tarefas, acabamento e revisão | Concluída |

## v1.1 (12/11/2026)

| Item | Estado |
|---|---|
| Tarefas que se repetem | Concluída |
| Widget de tela inicial | Concluída |
| Liquid Glass só nas superfícies flutuantes (nav pill, avisos, folhas) | Concluída |

## v1.2 (17/11/2026)

| Item | Estado |
|---|---|
| Atualizar por cima sem perder dados (release falha sem a chave, regra de migração, checklist) | Concluída |
| "Próximos dias" no Hoje e no widget (padrão 7 dias; 3/7/14/30 em Ajustes) | Concluída |
| Foco: manter a tela ligada | Concluída |
| Foco: descanso de tela em paisagem | Concluída |
| Foco: bloquear com o timer por cima da tela de bloqueio (serviço de acessibilidade) | Concluída |
| Foco: Não perturbe nas fases de foco, com apps liberados | Concluída |
| Foco: atalho da playlist lofi no YouTube | Concluída |

Decisões da v1.2: bloqueio por serviço de acessibilidade (não administrador do dispositivo, para
digital e rosto continuarem desbloqueando); opções novas do foco desligadas por padrão; nada
novo no banco (só DataStore).

## Fora do escopo

- **Acompanhamentos / follow-ups** ("cobrar fulano", "aguardando resposta de…"). O app é de uso
  pessoal; recursos de cobrança de terceiros não serão feitos, nem depois da v1.
- Backend e contas.

## Decisões

- **Compromissos sem participantes.** O Pulso não adiciona contatos nem envia convites; não pede
  permissão de contatos. Participantes de eventos que já existem no Google Agenda aparecem no
  detalhe e são preservados ao editar.
