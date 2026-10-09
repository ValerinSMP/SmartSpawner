# SmartSpawner — merge completo de upstream

Fecha: 2026-10-09. Desarrollo en SSH personal.
Rama de trabajo: work/valerin-upstream-26.2.
Fork: https://github.com/ValerinSMP/SmartSpawner.git.
Rama valerin conservada en 735c77a3782ce29c557cfb41b743d2ae8d87e3da.
Upstream confirmado por parent/source GitHub: OpenVdra/SmartSpawner, main.
Base inicial: 9cdf8bef4265f738c03ab2ce0662d6aa2728702c.
Autorización actual: aplicar todos los cambios pendientes de upstream.
Integrado íntegramente 08f4e8b86565dacddc368865258c083b3cf0219b.
Merge completo: 3c72d9f8ccc18f2c4f8a1c27ccaf473b9fbd6c4c, sin conflictos.
Cero commits de ese upstream pendientes; es ancestro de HEAD.
La única modificación propia previa registrada era VALERIN_FORK.md.
VALERIN_FORK.md intacto; SHA-256 6697824c62e3aa06b5be8730d24df32c9e8cafe0084856ba3d29e1ef7cbafade.
DEVELOPMENT_HANDOFF.md intacto, sin seguimiento ni añadido a commits;
SHA-256 4df0d596f2b4c23c983c4c8721cfa518bb78b5606038d4d9110af8e5e42dcb48.

## Resultado

SmartSpawner 1.8.3. Integradas correcciones de duplicación/menú fantasma,
almacenamiento con un solo jugador, avisos limitados, custom_spawners, aliases,
display_name, cabezas/drop chance por spawner, nuevo /ss give y recarga de loot.
Integrada corrección de delays, esquema SQL v5/custom_delay y actualizaciones
de AuraSkills, zMenu, SLF4J, checker-qual, Gradle 9.8.0 y mc-run.
Java/release 25 y API Paper 1.21.11 conservados, api-version 1.21.
El commit titulado target 26.3 añade 26.3 al listado del workflow de publicación;
mcRun y README siguen apuntando a Minecraft 26.2. No se ejecutó ese workflow.

## Validación

Comando mediante /root/development/tools/run-personal-build.sh:
./gradlew :core:test :core:shadowJar --no-daemon --no-parallel --max-workers=1
BUILD SUCCESSFUL; 82 tests, cero fallos, errores u omitidos.
git diff --check correcto. Bot del host healthy antes/después.
Avisos upstream de Shadow sobre DuplicatesStrategy de servicios: inspeccionados
los descriptores del JAR; todos sus proveedores referenciados tienen clase.
Eso no sustituye una conexión MySQL ni prueba de gameplay.

Artefacto: core/build/libs/SmartSpawner-1.8.3.jar.
Candidato: /root/development/artifacts/SmartSpawner/1.8.3-upstream-08f4e8b/SmartSpawner-1.8.3.jar.
SHA-256: 600294fdadbfd31190e15430fb04ca1add15749e5f7a6ede039941c9ddeb3f6b.
Candidato anterior 1.8.2 conservado.

## Límites y siguiente paso

Modificado, compilado y tests unitarios aprobados. No instalado ni activado.
No se ejecutaron migraciones contra YAML o bases de servidores.
Validar en Test Leaf 26.2: migración de archivos 1.8 y nombres/items/stacking
legados, aliases, custom spawners, reload de loot, almacenamiento con dos
jugadores, cierre de menús, venta/retirada y persistencia tras reinicio.
La migración crea copias .1.8-backup y añade custom_delay en esquema v5.
Los delays individuales anteriores vuelven una vez a la configuración;
reaplicarlos donde proceda. Respaldar configuración y BD antes de activar:
el backup YAML upstream registra un aviso pero no aborta si falla la copia.
Los tests actuales no demuestran migración MySQL ni comportamiento de GUIs.
No usar downgrade como reversión sin restauración coordinada de datos/config.

Publicación pendiente: el push HTTPS anterior carecía de autenticación; en
personal no se detectó gh ni credential.helper, y SSH no tiene host verificado.
No se copiaron credenciales ni modificó SSH. Sin reset/force-push/JAR oficial.
