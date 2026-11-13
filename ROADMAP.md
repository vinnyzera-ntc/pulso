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

## Depois da v1

- **Interface Liquid Glass só nas superfícies flutuantes** (decidido em 12/11/2026): nav pill,
  folhas e cartões flutuantes ganham estilo Liquid Glass (translúcido, com desfoque e refração).
  O resto do app continua no Pulso (superfícies opacas com hairlines), mantendo os papéis de cor
  (ação, tempo, concluído, atraso).
- Widget de tela inicial.

## Fora do escopo

- **Acompanhamentos / follow-ups** ("cobrar fulano", "aguardando resposta de…"). O app é de uso
  pessoal; recursos de cobrança de terceiros não serão feitos, nem depois da v1.
- Backend e contas.

## Decisões

- **Compromissos sem participantes.** O Pulso não adiciona contatos nem envia convites; não pede
  permissão de contatos. Participantes de eventos que já existem no Google Agenda aparecem no
  detalhe e são preservados ao editar.
