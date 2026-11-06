# Prompt de IA para criar uma skin v1

Substitua somente os valores entre colchetes e cole o texto abaixo na IA. A resposta dela deve ser o JSON, não uma explicação.

```text
Crie um único JSON UTF-8 válido e completo para uma skin da comunidade do Smart Island. Responda somente com o JSON puro, sem Markdown, sem cercas de código, sem explicações e sem texto antes ou depois.

Use exatamente estes sete campos na raiz, nesta versão: version, id, name, author, license, baseStyle, states. Não adicione campos extras em nenhum nível. version deve ser o número inteiro 1.

Dados do autor fornecidos por mim:
- id: [id-em-minusculas, 1-64 caracteres, somente a-z 0-9 _ -, começando por letra ou número]
- name: [nome da skin, até 80 caracteres]
- author: [minha autoria real ou meu handle, até 80 caracteres]
- license: [licença que eu realmente posso declarar, por exemplo MIT]
- baseStyle: [classic|obsidian|titanium|black_hole|eclipse|lightspeed|saturn]
- conceito visual original: [descreva cores e clima sem pedir cópia de marca, site ou aplicativo]

Em states, inclua obrigatoriamente idle, music, notification e call. Você pode incluir somente estes estados adicionais: battery, navigation, live_activity, transfer, hotspot, bluetooth, flashlight, screen_recording, timer, stopwatch. Em cada estado use somente colors e animation. colors deve ter de 1 a 4 strings em #RRGGBB ou #AARRGGBB, com alfa primeiro quando houver oito dígitos. animation deve conter exatamente type e speed; type só pode ser none, pulse, flow ou orbit; speed deve ser número finito entre 0.25 e 3.0.

Não use imagens, SVG, CSS, HTML, JavaScript, Kotlin, shader, scripts, URLs, caminhos de arquivos, gradientes CSS, nomes de cor, campos de descrição ou metadados extras. Não invente atribuição, licença ou autorização de terceiros. Se eu fornecer uma referência visual, adapte somente o que este schema representa e não prometa reprodução idêntica: ele não cria novo renderizador nem transporta imagens.
```

Revise o resultado com a [referência do schema](README.md) antes de importá-lo. A IA pode gerar JSON sintaticamente correto, mas não confirma autoria, licença, contraste ou equivalência visual com uma referência externa.
