# SmartSpawner — actualización focalizada a Minecraft 26.2

Fecha: 2026-10-09. Trabajo: merge upstream en personal.

- Fork: https://github.com/ValerinSMP/SmartSpawner.git; rama de partida , .
- Upstream confirmado por parent/source de GitHub: https://github.com/OpenVdra/SmartSpawner.git, rama .
- Base común: . La diferencia propia previa era únicamente ; no había modificaciones Java propias registradas.
- Integrado mediante merge real, sin conflictos: ; merge , rama .
- Ya integrados en la base: Java/release 25, API Paper 1.21.11, README con rango 1.21.6–26.2, soporte Paper/Folia y scheduler correspondiente.
- Cambios recibidos: runServer de 26.1.2 a 26.2; Lombok 1.18.48, checker-qual 4.2.3, slf4j-api 2.0.19 e instrucciones upstream. No cambia la API mínima, bytecode ni lógica de gameplay.
- Upstream observado: . Los commits posteriores al objetivo 26.2 quedan fuera: contienen funcionalidades/configuración nuevas y objetivo 26.3, además de correcciones posteriores que deben revisarse por separado.
- Preservados:  íntegro y  sin seguimiento ni modificaciones; SHA-256 del handoff .

## Validación y entrega

Ejecutado mediante :
To honour the JVM settings for this build a single-use Daemon process will be forked. For more on this, please refer to https://docs.gradle.org/9.7.1/userguide/gradle_daemon.html#sec:disabling_the_daemon in the Gradle documentation.
Daemon will be stopped at the end of the build 
> Task :api:compileJava UP-TO-DATE
> Task :api:processResources NO-SOURCE
> Task :api:classes UP-TO-DATE
> Task :core:compileJava UP-TO-DATE
> Task :core:processResources UP-TO-DATE
> Task :core:classes UP-TO-DATE
> Task :core:shadowJar UP-TO-DATE
> Task :core:compileTestJava UP-TO-DATE
> Task :core:processTestResources NO-SOURCE
> Task :core:testClasses UP-TO-DATE
> Task :core:test UP-TO-DATE

BUILD SUCCESSFUL in 5s
6 actionable tasks: 6 up-to-date
Consider enabling configuration cache to speed up this build: https://docs.gradle.org/9.7.1/userguide/configuration_cache_enabling.html.
BUILD SUCCESSFUL; 82 tests, cero fallos, errores u omitidos.  correcto.
El bot del host permaneció healthy antes/después. El AGENTS upstream dice que no hay tests, pero el árbol real contiene tests y se ejecutaron.

Artefacto del fork: .
SHA-256: .
Candidato durable: .

Modificado, compilado y tests unitarios aprobados. No instalado ni activado.
Leaf/Minecraft 26.2 y gameplay siguen pendientes de validación en Test: arranque, carga/persistencia SQLite o MySQL según destino, colocación/rotura/stack, generación y almacenamiento, retirada/venta, permisos/protecciones y reinicio/reconexión.
No se descargó un JAR oficial ni se ejecutó reset, force-push o lifecycle de servidores.
