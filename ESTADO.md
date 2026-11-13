# Pulso — estado atual (12/11/2026)

Referência rápida para retomar o trabalho. Plano completo das fases em `ROADMAP.md`.

## Onde estamos

- **Fases 0 a 5 concluídas** e commitadas em `main`. A v1 está completa; fase 5 aprovada pelo usuário em 12/11/2026.
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
- **O release 1.0.0 está instalado no aparelho de teste desde 12/11/2026** (no lugar do debug, que não tinha tarefas reais). Permissões de agenda e notificações concedidas pelo usuário.
- Teste de reinício feito no aparelho com o release: o alarme foi rearmado sozinho após o boot e o lembrete tocou no horário (22:31), sem abrir o app. O teste de troca de fuso foi dispensado pelo usuário (sem viagem prevista); o comportamento está coberto por testes unitários.
- Atenção: o adb não consegue usar `run-as` nem o seed de debug no release. Para testes com seed, instalar o debug exigiria desinstalar o release (apaga os dados reais). Testes no aparelho daqui em diante devem ser feitos pela interface, sem apagar dados do usuário.
- Minificação (R8) segue desligada no release.

## Aparelho de teste

- Aparelho de teste. Configurações restauradas ao fim da fase 5: night mode = yes, `stay_on_while_plugged_in` = 0, `animator_duration_scale` = 1.0, `user_rotation` = 0, `font_scale` = 1.0.
- Pacote de teste desinstalado; seed `limpar` rodado (sem tarefas de exemplo nem agenda "Pulso Teste").
- Para dirigir o app: `source scripts/ui.sh`, `scripts/prints.sh`, seed de debug (`-e seed demo|demo-proximo|foco:<min>|lembrete:<min>|recentes|limpar`). `center`/`tap` e `tap_text.sh` procuram primeiro o texto visível (o círculo de concluir tem descrição "Concluir: <título>").

## Próximos passos

### Depois da v1
- Interface Liquid Glass **só nas superfícies flutuantes** (nav pill, folhas, cartões flutuantes); o resto segue no Pulso. Decidido em 12/11/2026.
- Widget de tela inicial.

### Fora do escopo (definitivo)
- Follow-ups/acompanhamentos de terceiros; backend e contas.
