# Pulso — estado atual (12/11/2026)

Referência rápida para retomar o trabalho. Plano completo das fases em `ROADMAP.md`.

## Onde estamos

- **Fases 0 a 4 concluídas** e commitadas em `main` (último commit: `e7b95f6 feat: fase 4 — Captura rápida e Modo foco`).
- **Fase 5 não iniciada.** A fase 4 está aguardando o OK do usuário (fluxo: cada fase termina com commit + prints e para para aprovação).
- Testes na entrega da fase 4: 107 unitários (73 casos de tabela no parser) e 14 instrumentados no aparelho.
- Prints da fase 4 em `prints/fase-4` (gitignored): captura, criação de tarefa/compromisso, foco (preparação, rodando, pausado, pausa, concluído) e paisagem.

## O que o app já faz

| Área | Conteúdo |
|---|---|
| Base | Tema Pulso (Compose + Material 3, dark-first), Room, DataStore, backup automático ligado |
| Agenda Google | Via Calendar Provider (sem backend); dia, semana e mês; criar/editar/apagar; recorrentes com "Só este / Toda a série"; sincronização pedida logo após gravar |
| Hoje e Tarefas | Lista do dia, tarefas com data/prioridade/etiquetas, detalhe da tarefa |
| Captura rápida | Parser pt-BR local (datas, horas, duração, #etiquetas, p1–p4; repetição reconhecida mas não suportada); "Entendi assim", troca Tarefa/Compromisso, recentes, Desfazer |
| Modo foco | Pomodoro (FocusMachine), serviço em primeiro plano com contagem na notificação, fim de fase por alarme exato (funciona com tela apagada), sessões no Room, FocusOrbit com animação contínua |
| Paisagem | Layouts em duas colunas em Agenda, Captura e Foco |

## Aparelho de teste

- Aparelho de teste **conectado e acessível via adb** em 12/11/2026, desbloqueado.
- Configurações conferidas e no valor original: night mode = yes, `stay_on_while_plugged_in` = 0, `animator_duration_scale` = 1.0, `user_rotation` = 0.
- Pacote de teste `com.organizador.agenda.test` não está instalado (limpo). App `com.organizador.agenda` instalado.
- Para dirigir o app: `source scripts/ui.sh` (tem trava de primeiro plano), `scripts/prints.sh`, seed de debug (`-e seed demo|demo-proximo|foco:<min>|recentes|limpar`). Sempre `limpar` e restaurar configurações ao fim.

## Próximos passos

### Fase 5 — lembretes, acabamento e revisão (a fazer)
1. **Lembretes de tarefas**: notificação no horário da tarefa; tarefa com data sem hora avisa às 09:00 (ajustável em Ajustes); reagendar após reboot e mudança de fuso/hora; ações na notificação (concluir, adiar).
2. **Acabamento**: estados vazios e de erro, acessibilidade (TalkBack, tamanhos de fonte), revisar paisagem em todas as telas, ícone e nome do app.
3. **Revisão**: passada de código (`/code-review`), testes que faltarem, build de release assinado para instalação direta.
4. Fechamento: commit + prints em `prints/fase-5`, restaurar o aparelho e parar para OK.

### Pendências a esclarecer
- **Participantes de compromissos**: a memória registra que a decisão "sem participantes" foi **revogada em 11/11/2026**, mas o `ROADMAP.md` (seção Decisões) ainda a descreve como vigente. Confirmar com o usuário o que vale e atualizar o ROADMAP.

### Depois da v1
- Interface Liquid Glass (decidir antes: substitui o Pulso inteiro ou só superfícies flutuantes).
- Widget de tela inicial.

### Fora do escopo (definitivo)
- Follow-ups/acompanhamentos de terceiros; backend e contas.
