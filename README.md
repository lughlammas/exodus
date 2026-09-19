# Êxodus

**Software livre (MIT)** — gerenciador de arquivos Android simples: sem anúncios, sem rastreadores, sem lock-in.

> Todos são ciganos: caminham de um lugar ao outro com bom comando e boa direção.

| | |
|--|--|
| **applicationId** | `com.lughlammas.exodus` |
| **Nome** | Êxodus |
| **Versão** | 1.0.0 |
| **Licença** | MIT |

## O que faz

1. Navegar pastas (Download, Documents, armazenamento amplo no Android 11+)
2. Listar nome, tamanho e data
3. Zipar uma pasta (saída limpa com `ZipOutputStream`)
4. Compartilhar o ZIP pelo sistema
5. Copiar caminho, criar pasta, renomear, apagar (com confirmação)
6. Abrir arquivos com intents do sistema
7. UI Material escura

## Instalar (APK)

1. Baixe `Exodus.apk` na [Release](https://github.com/lughlammas/exodus/releases) mais recente.
2. Permita instalar apps de fonte desconhecida, se o Android pedir.
3. Abra o APK e confirme.

## Compilar

JDK 17 + Android SDK (compileSdk 35).

```bash
./gradlew assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

Crie `local.properties` com o caminho do SDK (não versionado).

## Permissões

- Armazenamento / “Todos os arquivos” (`MANAGE_EXTERNAL_STORAGE` no Android 11+) para listar e zipar fora do sandbox.
- Sem internet, sem localização, sem anúncio.

## Autor

Lab **lughlammas** — Guilherme / Gui.  
Portfolio: [lughlammas.github.io](https://lughlammas.github.io)
