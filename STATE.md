# SmartSpawner — actualización focalizada a Minecraft 26.2

Fecha: 2026-10-09. Trabajo en SSH personal.
Rama: work/valerin-upstream-26.2.
Fork: https://github.com/ValerinSMP/SmartSpawner.git.
Partida: valerin, 735c77a3782ce29c557cfb41b743d2ae8d87e3da.
Upstream real (parent/source GitHub): OpenVdra/SmartSpawner, main.
Base común: 9cdf8bef4265f738c03ab2ce0662d6aa2728702c.
La única diferencia propia registrada antes del merge era VALERIN_FORK.md.

Merge real sin conflictos: db260e3d9900aeb3121d973509dc3f202943dcfb.
Objetivo integrado: 65c8f00fb75a99fa53d9d2ed4201bfcfae2fd325.
Ya estaban integrados Java/release 25, API Paper 1.21.11 y rango declarado hasta 26.2.
Recibido: runServer 26.2, Lombok 1.18.48, checker-qual 4.2.3,
slf4j-api 2.0.19 e instrucciones upstream. Sin cambios de mecánicas.
Upstream actual observado: 08f4e8b86565dacddc368865258c083b3cf0219b.
Los commits posteriores al objetivo quedan pendientes por separado: nuevas
funciones/configuración, correcciones de almacenamiento y objetivo 26.3.

VALERIN_FORK.md conservado íntegro.
DEVELOPMENT_HANDOFF.md conservado sin seguimiento y sin modificaciones.
SHA-256 handoff: 4df0d596f2b4c23c983c4c8721cfa518bb78b5606038d4d9110af8e5e42dcb48.

Validación mediante /root/development/tools/run-personal-build.sh:
./gradlew :core:test :core:shadowJar --no-daemon --no-parallel --max-workers=1
BUILD SUCCESSFUL; 82 tests, cero fallos, errores u omitidos.
git diff --check correcto; bot healthy antes/después.
El AGENTS upstream dice que no hay tests; el árbol real sí contiene tests.
Un error de escape al generar este documento lanzó además un Gradle incremental
fuera del wrapper; terminó en 5 segundos, todas las tareas up-to-date.
Documento corregido; no se modificaron servicios ni fuentes por ese error.

JAR del fork: core/build/libs/SmartSpawner-1.8.2.jar.
Candidato: /root/development/artifacts/SmartSpawner/1.8.2-upstream-26.2/SmartSpawner-1.8.2.jar.
SHA-256: db325648d0c69d9cabcbebe399547750c08d0e0c63dac6c30189ff98d881ca6d.

Modificado, compilado y tests aprobados. No instalado ni activado.
Leaf 26.2/gameplay pendientes: arranque, persistencia, colocar/romper/stack,
generación, almacenamiento, retirada/venta, permisos y reinicio/reconexión en Test.
No JAR oficial, reset ni force-push. valerin se conserva en su HEAD original.
Push HTTPS rechazado por falta de autenticación; SSH no tiene host verificado.
Publicación pendiente; no se alteraron credenciales ni configuración SSH.
