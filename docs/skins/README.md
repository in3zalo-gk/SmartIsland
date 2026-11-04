# Skins da comunidade — referência do schema v1

Uma skin é um único arquivo JSON UTF-8, declarativo, de no máximo **24 KiB (24.576 bytes)**. Ela muda apenas a decoração da ilha: não instala renderizadores, não carrega imagens, não executa scripts e não baixa URLs. A implementação e os avisos de licença GNU GPL do aplicativo permanecem inalterados.

- Para criar, importar, testar e guardar uma cópia do arquivo no celular, siga o [tutorial prático](TUTORIAL.md).
- Para pedir um arquivo compatível a uma IA, copie o [prompt pronto](PROMPT-IA.md).
- Veja um arquivo válido e original em [`community/skins/aurora-safe.json`](../../community/skins/aurora-safe.json). A licença dele vale somente para esse exemplo; não presume licença para outros trabalhos.

## Raiz obrigatória

A raiz deve conter **exatamente** estes sete campos, sem nenhum extra:

| Campo | Regra real |
| --- | --- |
| `version` | número inteiro `1` (não texto) |
| `id` | 1–64 caracteres: minúsculas ASCII, números, `_` ou `-`; começa com letra ou número |
| `name` | texto não vazio de até 80 caracteres |
| `author` | texto não vazio de até 80 caracteres; autoria real ou handle do autor |
| `license` | texto não vazio de até 80 caracteres; licença que o autor realmente pode declarar |
| `baseStyle` | um dos estilos permitidos abaixo |
| `states` | objeto com 1–14 estados, contendo obrigatoriamente `idle` |

`id`, `name`, `author` e `license` não podem incluir URL, `www.`, barra (`/` ou `\\`) nem `..`. A licença no JSON é só metadado: não baixa nem prova uma autorização. Não declare autoria de terceiros, nem use nome, arte ou identidade visual de marca alheia como se fossem seus.

## Valores permitidos

- `baseStyle`: `classic`, `obsidian`, `titanium`, `black_hole`, `eclipse`, `lightspeed` ou `saturn`.
- Estados: `idle`, `music`, `notification`, `call`, `battery`, `navigation`, `live_activity`, `transfer`, `hotspot`, `bluetooth`, `flashlight`, `screen_recording`, `timer`, `stopwatch`.
- Animações: `none`, `pulse`, `flow` ou `orbit`.

Estados que não existirem no arquivo usam `idle`. A skin não cria música, chamada, notificação, permissão ou serviço: apenas decora o estado real que o aplicativo receber.

Cada estado é um objeto que aceita somente `colors` e/ou `animation`:

- `colors` é opcional; se presente, é uma lista de **1 a 4** textos no formato ARGB `#RRGGBB` (opaco, com `FF` implícito) ou `#AARRGGBB` (alfa primeiro). Exemplos válidos: `#38BDF8` e `#CC38BDF8`.
- `animation` é opcional; se presente, contém **exatamente** `type` e `speed`. `speed` é um número finito entre `0.25` e `3.0`, inclusive.
- Se `colors` ou `animation` for omitido, o renderizador usa o respectivo padrão local. Não há CSS, gradiente CSS, cor por nome, SVG, HTML, PNG, shader, script, URL ou caminho de arquivo.

## Exemplo mínimo completo

```json
{
  "version": 1,
  "id": "minha-primeira-skin",
  "name": "Minha primeira skin",
  "author": "SeuHandle",
  "license": "MIT",
  "baseStyle": "classic",
  "states": {
    "idle": {
      "colors": ["#38BDF8", "#10B981"],
      "animation": {"type": "none", "speed": 1.0}
    },
    "music": {
      "colors": ["#A78BFA", "#F472B6"],
      "animation": {"type": "pulse", "speed": 0.75}
    },
    "notification": {
      "colors": ["#F59E0B"],
      "animation": {"type": "flow", "speed": 0.5}
    },
    "call": {
      "colors": ["#34D399"],
      "animation": {"type": "orbit", "speed": 0.5}
    }
  }
}
```

## Limites e licença

Os estilos base são tratamentos locais do Compose; seus nomes não concedem assets, código ou aparência exata de qualquer outro produto. Uma IA tampouco consegue garantir identidade visual de um site, aplicativo ou marca de terceiros com este schema, pois ele não transporta imagens nem cria um novo renderizador.

Antes de compartilhar, valide em aparelho real `idle`, música, notificação e chamada; teste movimento reduzido/animações desligadas, tema claro e escuro, ilha recolhida e expandida, retrato e paisagem. A prévia não substitui esses testes.

Ao contribuir, envie JSON, licença e informação de autoria verdadeira. Mantenha os avisos GPL já existentes nos arquivos do aplicativo: dados de exemplo podem ter licença própria explícita, mas mudanças no código continuam sob a licença do projeto.
