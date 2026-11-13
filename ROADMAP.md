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
| 5 | Lembretes de tarefas, acabamento e revisão | A fazer |

## Depois da v1

- **Interface Liquid Glass.** Trocar a linguagem visual atual (Pulso: superfícies opacas com
  hairlines) por um estilo Liquid Glass (superfícies translúcidas com desfoque e refração).
  A decidir antes de começar: se substitui o Pulso por inteiro ou só as superfícies flutuantes
  (nav pill, folhas, cartões), mantendo os papéis de cor (ação, tempo, concluído, atraso).
- Widget de tela inicial.

## Fora do escopo

- **Acompanhamentos / follow-ups** ("cobrar fulano", "aguardando resposta de…"). O app é de uso
  pessoal; recursos de cobrança de terceiros não serão feitos, nem depois da v1.
- Backend e contas.

## Decisões

- **Compromissos sem participantes.** O Pulso não adiciona contatos nem envia convites; não pede
  permissão de contatos. Participantes de eventos que já existem no Google Agenda aparecem no
  detalhe e são preservados ao editar.
