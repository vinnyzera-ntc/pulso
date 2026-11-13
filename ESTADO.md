# Pulso — estado atual (12/11/2026)

Referência rápida para retomar o trabalho. Plano completo das fases em `ROADMAP.md`.

## Onde estamos

- **Fases 0 a 5 concluídas** e commitadas em `main`. A v1 está completa e **aguarda o OK do usuário** para a fase 5.
- Testes na entrega da fase 5: 122 unitários e 15 instrumentados no aparelho, todos passando.
- Prints da fase 5 em `prints/fase-5` (gitignored): telas nos dois temas (`folha-temas.png`), notificação de lembrete, toque abrindo a tarefa, fonte 2× (`fonte-2x/`) e paisagem (`paisagem/`).

## O que o app já faz

| Área | Conteúdo |
|---|---|
| Base | Tema Pulso (Compose + Material 3, dark-first), Room, DataStore, backup automático ligado |
| Agenda Google | Via Calendar Provider (sem backend); dia, semana e mês; criar/editar/apagar; recorrentes com "Só este / Toda a série"; sincronização pedida logo após gravar; falha ao gravar vira aviso, sem fechar o app |
| Hoje e Tarefas | Lista do dia, tarefas com data/prioridade/etiquetas, detalhe da tarefa |
| Lembretes de tarefas | Um alarme exato para o próximo lembrete; notificação com Concluir, Adiar 15 min e Adiar 1 h; tocar abre a tarefa; sem hora = 09:00 (Ajustes); rearma ao religar, atualizar o app e mudar hora/fuso; lembrete perdido com o aparelho desligado toca ao religar, horário que já passou ao criar não toca |
| Captura rápida | Parser pt-BR local (datas, horas, duração, #etiquetas, p1–p4; repetição reconhecida mas não suportada); "Entendi assim", troca Tarefa/Compromisso, recentes, Desfazer |
| Modo foco | Pomodoro (FocusMachine), serviço em primeiro plano com contagem na notificação, fim de fase por alarme exato, sessões no Room, FocusOrbit com animação contínua |
| Acabamento | Ícone adaptativo próprio (linha de pulso ember + ponto volt, com versão monocromática); fonte grande (até 2×) sem quebras; rótulos do TalkBack nos círculos de concluir ("Concluir: título"); paisagem revisada em todas as telas |

## Release

- `./gradlew :app:assembleRelease` gera `app/build/outputs/apk/release/app-release.apk`, versão **1.0.0**, assinado.
- Chave em `keystore/pulso-release.jks`, senhas em `keystore.properties` (os dois **fora do git**). **Fazer cópia de segurança dos dois**: sem eles não é possível instalar atualizações por cima.
- O aparelho de teste tem o build **debug** (outra assinatura). Para trocar pelo release é preciso desinstalar o debug antes, o que apaga as tarefas locais (as agendas do Google não são afetadas).
- Minificação (R8) segue desligada no release.

## Aparelho de teste

- Aparelho de teste. Configurações restauradas ao fim da fase 5: night mode = yes, `stay_on_while_plugged_in` = 0, `animator_duration_scale` = 1.0, `user_rotation` = 0, `font_scale` = 1.0.
- Pacote de teste desinstalado; seed `limpar` rodado (sem tarefas de exemplo nem agenda "Pulso Teste").
- Para dirigir o app: `source scripts/ui.sh`, `scripts/prints.sh`, seed de debug (`-e seed demo|demo-proximo|foco:<min>|lembrete:<min>|recentes|limpar`). `center`/`tap` e `tap_text.sh` procuram primeiro o texto visível (o círculo de concluir tem descrição "Concluir: <título>").

## Próximos passos

### Depois da v1
- Interface Liquid Glass (decidir antes: substitui o Pulso inteiro ou só superfícies flutuantes).
- Widget de tela inicial.

### Fora do escopo (definitivo)
- Follow-ups/acompanhamentos de terceiros; backend e contas.
