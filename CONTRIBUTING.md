# Como contribuir

Obrigado pelo interesse no Pulso! O app nasceu para uso pessoal, então o escopo é enxuto de
propósito. Antes de investir tempo num recurso novo, abra uma issue para conversarmos.

## Relatar um problema

Abra uma [issue](https://github.com/vinnyzera-ntc/pulso/issues) com:

- versão do Pulso (Ajustes) e do Android, e o modelo do aparelho;
- o que você fez, o que esperava e o que aconteceu;
- prints ou gravação de tela, se ajudar (sem dados pessoais).

## Enviar uma mudança

1. Faça um fork e crie um branch a partir de `main`.
2. Mantenha a mudança pequena e focada num assunto só.
3. Rode os testes unitários antes de abrir o pull request:
   ```bash
   ./gradlew :app:testDebugUnitTest
   ```
   Se mexer no banco (Room), em agenda ou em DAOs, rode também os instrumentados com um
   aparelho ou emulador conectado: `./gradlew :app:connectedDebugAndroidTest`.
4. Mudanças no esquema do banco precisam de migração e do novo JSON em `app/schemas/`.
5. Mensagens de commit no formato `tipo: descrição` (`feat`, `fix`, `docs`, `refactor`,
   `test`, `chore`), em português.
6. Abra o pull request explicando o que mudou e por quê, com prints se a interface mudou.

## Estilo

- Kotlin com as convenções oficiais; interface em Jetpack Compose + Material 3 seguindo o
  tema Pulso (`ui/pulso`).
- Textos da interface em português do Brasil.
- Lógica de domínio (datas, repetição, parser da captura) fica em `domain/` e deve vir com
  testes unitários.

## Fora do escopo

Para manter o app simples, não serão aceitos: backend ou contas de usuário, sincronização
própria na nuvem, anúncios, rastreamento, e recursos de cobrança/acompanhamento de terceiros.

Ao contribuir, você concorda que seu código será distribuído sob a [licença MIT](LICENSE) e
com o [código de conduta](CODE_OF_CONDUCT.md).
