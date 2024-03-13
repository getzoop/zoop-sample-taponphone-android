# zoop-sample-taponphone-android
Se você é desenvolvedor (ou tem acesso à uma equipe de desenvolvimento) e deseja criar seu próprio aplicativo para cobranças utilizando o TapOnPhone Zoop, que transforma seu celular android em uma maquininha de cartão.

Você pode fazer forks do projeto ou simplesmente criar seu próprio repositório com o conteúdo alterado. Como esta é uma solução open-source, queremos o apoio da comunidade. Correções e PRs são super bem vindos.

Caso note algum problema com seu funcionamento, entre em contato com nosso suporte através do e-mail: suporte@zoop.com.br.

## Configuração do sample

Adicione as credenciais `MARKETPLACE_ID`,`SELLER_ID`, `API_KEY`, `CLIENT_ID`, `CLIENT_SECRET` no `local.properties` do projeto com os devidos valores.

Exemplo com valores fictícios:

```
sdk.dir=/Users/zoop/Library/Android/sdk

debug.CLIENT_ID="1"
debug.CLIENT_SECRET="2"
debug.API_KEY="3"
debug.MARKETPLACE="4"
debug.SELLER="5"

release.CLIENT_ID="1"
release.CLIENT_SECRET="2"
release.API_KEY="3"
release.MARKETPLACE="4"
release.SELLER="5"
```

Caso tenha dúvidas em relação a essas credenciais entrar em contato com o suporte. 

## Credenciais para download da SDK

A sdk é baixada através de um repositório maven referenciado no `build.gradle.kts` raiz do projeto.

As variáveis de ambiente `GITHUB_USER`,`GITHUB_PAT` devem ser configuradas de acordo com as instruções do repositório maven de pacotes públicos da Zoop:
https://github.com/getzoop/zoop-package-public


## Configuração de Ambiente

A SDK da Zoop é distribuida em duas versões, release e debug.
Para transacionar em ambiente de staging, utilize build de debug, já para o ambiente de produção, utilize o build de release.

Para diferentes ambientes, as credenciais também são diferentes, certifique-se de que está usando as credenciais corretas para o ambiente alvo.

## Licença

zoop-sample-taponphone-android está licenciada sob os termos da licença [MIT License](LICENSE) e está disponível gratuitamente.

## Links

* [Documentação](https://getzoop.github.io/zoop-sdk-taponphone-android/)
* [Suporte](suporte@zoop.com.br)