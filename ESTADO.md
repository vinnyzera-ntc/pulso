# Pulso — estado atual (12/11/2026)

Referência rápida para retomar o trabalho. Plano completo das fases em `ROADMAP.md`.

## Onde estamos

- **v1 (fases 0 a 5) concluída e aprovada** em 12/11/2026.
- **v1.1 (12/11/2026)**: tarefas que se repetem, widget de tela inicial e Liquid Glass nas superfícies flutuantes. Instalada no aparelho de teste (release 1.1.0, versionCode 2), aguardando avaliação do usuário.
- Testes: 138 unitários e 16 instrumentados (inclui a migração do banco v1 → v2), todos passando.
- Prints da v1.1 em `prints/v1.1` (gitignored): repetição (captura, lista, folha, concluir), vidro (`glass-folha.png`) e widget claro/escuro.
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
| Tarefas que se repetem | Diária, dias úteis, dias da semana, mensal (dia N, último dia em mês curto), anual, "a cada N". Concluir leva à próxima data (atrasada pula para depois de hoje), desmarca subtarefas e reativa lembretes; aviso "Próxima: …" com Desfazer. Captura entende "toda segunda e quarta", "todo dia 5", "a cada 2 semanas", "dias úteis". Compromissos que se repetem continuam pelo Google Agenda |
| Widget | "Hoje" (Glance): data, progresso, compromisso de agora/próximo, tarefas abertas com círculo de concluir, "+N mais", "+" abre a Captura; cores do Pulso claro/escuro; atualiza quando dados mudam e nas viradas de compromisso/meia-noite |
| Liquid Glass | Só nas superfícies flutuantes: nav pill, avisos e folhas (desfoque + refração nas bordas + brilho especular; Kyant Backdrop 1.0.6). Resto do app no Pulso opaco. Android < 12: visual opaco |
| Acabamento | Ícone adaptativo próprio (linha de pulso ember + ponto volt, com versão monocromática); fonte grande (até 2×) sem quebras; rótulos do TalkBack nos círculos de concluir ("Concluir: título"); paisagem revisada em todas as telas |

## Release

- `./gradlew :app:assembleRelease` gera `app/build/outputs/apk/release/app-release.apk`, versão **1.1.0** (versionCode 2), assinado. Atualizações: subir versionCode e instalar por cima (`adb install -r`), sem desinstalar.
- Chave em `keystore/pulso-release.jks`, senhas em `keystore.properties` (os dois **fora do git**). **Fazer cópia de segurança dos dois**: sem eles não é possível instalar atualizações por cima.
- **O release 1.0.0 está instalado no aparelho de teste desde 12/11/2026** (no lugar do debug, que não tinha tarefas reais). Permissões de agenda e notificações concedidas pelo usuário.
- Teste de reinício feito no aparelho com o release: o alarme foi rearmado sozinho após o boot e o lembrete tocou no horário (22:31), sem abrir o app. O teste de troca de fuso foi dispensado pelo usuário (sem viagem prevista); o comportamento está coberto por testes unitários.
- O build **debug** agora é outro pacote (`com.organizador.agenda.debug`, "Pulso debug") e convive com o release: testes instrumentados e seeds rodam nele sem tocar nos dados reais. Os scripts (`ui.sh`, `prints.sh`) usam o debug por padrão; `PULSO_PKG=com.organizador.agenda` para dirigir o release. Desinstalar o debug ao fim (a agenda local "Pulso Teste" do seed aparece também no release: rodar `limpar` antes).
- Minificação (R8) segue desligada no release.

## Aparelho de teste

- Aparelho de teste. Configurações restauradas ao fim da fase 5: night mode = yes, `stay_on_while_plugged_in` = 0, `animator_duration_scale` = 1.0, `user_rotation` = 0, `font_scale` = 1.0.
- Pacote de teste desinstalado; seed `limpar` rodado (sem tarefas de exemplo nem agenda "Pulso Teste").
- Para dirigir o app: `source scripts/ui.sh`, `scripts/prints.sh`, seed de debug (`-e seed demo|demo-proximo|foco:<min>|lembrete:<min>|recentes|limpar`). `center`/`tap` e `tap_text.sh` procuram primeiro o texto visível (o círculo de concluir tem descrição "Concluir: <título>").

## GitHub

- Repositório: https://github.com/vinnyzera-ntc/pulso (criado em 12/11/2026), licença MIT.
- README, CONTRIBUTING, CODE_OF_CONDUCT e prints de demonstração em `docs/screenshots`.
- Releases trazem o APK assinado (`pulso-<versão>.apk`) para instalar direto no celular. A cada
  versão nova: subir `versionCode`/`versionName`, `./gradlew :app:assembleRelease`, criar a tag
  `v<versão>` e anexar o APK com `gh release create`. Sempre com a mesma chave, senão o APK novo
  não instala por cima.

## Próximos passos

### Ideias em aberto (nenhuma combinada)
- Repetição também no formulário "Novo item" (hoje: Captura e detalhe da tarefa).
- Ligar a otimização R8 no release.

### Fora do escopo (definitivo)
- Follow-ups/acompanhamentos de terceiros; backend e contas.
