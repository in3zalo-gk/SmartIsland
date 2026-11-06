# Tutorial: criar e importar uma skin no celular

Consulte a [referência do schema v1](README.md) para todas as regras e, se quiser partir de uma ideia, use o [prompt de IA](PROMPT-IA.md). Este fluxo cria apenas decoração declarativa; não importa imagens nem instala código.

## 1. Criar e salvar o JSON

1. Abra um editor de texto que permita salvar arquivo simples no celular.
2. Copie o exemplo de `README.md` ou gere um rascunho com `PROMPT-IA.md`.
3. Troque `id`, `name`, `author` e `license` por dados reais seus. Escolha somente um `baseStyle` e estados/cores permitidos.
4. Salve como, por exemplo, `minha-primeira-skin.json`, em **UTF-8**, como texto simples — não `.txt`, documento rico, ZIP nem página HTML.
5. Mantenha esse arquivo original em uma pasta sua ou serviço de backup. Não existe um botão específico de exportação de skin na tela **Comunidade**; para guardar ou compartilhar a skin, copie/compartilhe o JSON original pelo gerenciador de arquivos.

Antes de importar, confira: raiz com exatamente sete campos; `version` numérico `1`; `idle` presente; cores com `#RRGGBB` ou `#AARRGGBB`; velocidade entre `0.25` e `3.0`; arquivo de no máximo 24 KiB. `music`, `notification` e `call` são recomendados para teste, mas somente `idle` é obrigatório pelo parser.

## 2. Importar na interface do Smart Island

1. Abra o Smart Island e toque em **Comunidade** na navegação inferior.
2. Na seção **Skins da comunidade**, toque em **Importar JSON**.
3. Escolha o arquivo `.json` salvo no seletor de arquivos do Android.
4. Confira a prévia e os metadados exibidos: nome, autor e licença.
5. Toque em **Salvar e aplicar**. Se o mesmo `id` já existir, a ação passa a ser **Substituir e aplicar**; use-a somente quando realmente quiser trocar a cópia local.
6. Para selecionar outra skin importada, toque em **Aplicar** na skin desejada. Para voltar aos estilos do aplicativo, use **Usar padrão integrado**. **Remover** apaga a cópia da biblioteca local, não o seu JSON original salvo fora do aplicativo.

A prévia é ilustrativa. Estados inexistentes no arquivo caem em `idle`, e a skin não cria atividades de música, chamada ou notificação.

## 3. Testar no aparelho

Faça o teste no próprio aparelho, pois a validação automática não substitui uso real:

- Confira `idle` ao deixar a ilha sem atividade.
- Inicie música e observe `music`.
- Receba uma notificação comum e confira `notification`.
- Faça ou receba uma chamada de teste e confira `call`.
- Teste ilha recolhida e expandida, retrato e paisagem, e ajuste de Disposição em tamanhos extremos.
- Teste tema claro e escuro, texto ampliado e movimento reduzido/animações desativadas. Com movimento reduzido, a prévia fica estática.
- Veja se texto, ícones, controles e o pet continuam legíveis e tocáveis; não use cor como único sinal de estado.
- Tente importar deliberadamente um JSON inválido e confirme que a skin anterior continua disponível.

Se algo não ficar bom, edite o JSON original, salve novamente em UTF-8 e importe de novo. Guarde cópias nomeadas por versão, como `minha-skin-v2.json`, para poder comparar e reverter.

## 4. Compartilhar com responsabilidade

Publique apenas trabalho original ou conteúdo para o qual você tenha permissão. Inclua o JSON, a licença aplicável e autoria verdadeira; uma licença declarada não autoriza copiar assets, código ou visual de terceiros. As referências a estilos são locais e conceituais: este formato não garante identidade visual de site alheio e não fornece imagens nem novo renderizador.

Se contribuir com o projeto, preserve os avisos GPL existentes no código. Um JSON original pode trazer sua própria licença explícita, sem mudar a licença do código do aplicativo.
