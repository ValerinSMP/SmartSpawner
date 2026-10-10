# SmartSpawner — upstream y recuperación de reglas Valerin

Fecha: 2026-10-09. SSH personal.
Rama: work/valerin-upstream-26.2.
Upstream 08f4e8b86565dacddc368865258c083b3cf0219b integrado completo.
Merge 3c72d9f8ccc18f2c4f8a1c27ccaf473b9fbd6c4c sin conflictos.
valerin permanece en 735c77a3782ce29c557cfb41b743d2ae8d87e3da.

Corregida la conclusión anterior: ese HEAD solo difería de upstream en un
documento, pero no contenía los parches del fork histórico que usaba el propietario.
Evidencia: archivos ya existentes en HomeSV bajo
/home/mtyn/servers/survival-custom/staging/decompiled/SmartSpawner-live/
github/nighter/smartspawner/spawner/gui/main/SpawnerMenuAction.java y
spawner/interactions/type/SpawnEggHandler.java.
No se decompilaron nuevos artefactos. Fuentes antiguas canónicas originales
de esos parches siguen sin localizar; el comportamiento se recuperó con esa referencia.

Recuperado Mending/EcoEnchants por claves mending, repairing, repair.
Recuperado bloqueo de cambio por huevo si stack supera límite configurable (default 1).
Guard antes del evento, mutación y consumo. Conservados callbacks upstream y
su corrección de no consumir huevo cuando se cancela el evento.
Añadidos config y mensaje en todos los locales; español en es_ES,
texto fallback inglés en restantes locales. Regresiones de claves y límites.

Build mediante /root/development/tools/run-personal-build.sh:
./gradlew :core:test :core:shadowJar --no-daemon --no-parallel --max-workers=1
BUILD SUCCESSFUL; 86 tests, cero fallos, errores u omitidos.
El primer intento de nuevos tests mostró falta del módulo API en su classpath;
resuelto con testImplementation(project(":api")), sin alterar shading.
Los tests prueban las reglas puras; no simulan un servidor EcoEnchants ni eventos
de inventario. Validación gameplay de reparación/XP y no consumo de huevo pendiente.

SmartSpawner 1.8.3. Candidato: /root/development/artifacts/SmartSpawner/1.8.3-valerin-mending-egg/SmartSpawner-1.8.3.jar
SHA-256: 2308273a90cc5d9f1fac0c21dc9cd94e14e95fe4df0b85b97de07425b0ba16e2
Candidatos anteriores conservados, pero carecen de estas dos reglas recuperadas.
DEVELOPMENT_HANDOFF.md sin seguimiento e intacto, SHA-256
4df0d596f2b4c23c983c4c8721cfa518bb78b5606038d4d9110af8e5e42dcb48.

No instalado, activado ni migrado sobre datos reales. Leaf 26.2 pendiente.
Antes de activación: probar legado/stacking y migraciones YAML/BD v5;
los delays individuales antiguos vuelven una vez al valor configurado.
Persisten avisos upstream Shadow de DuplicatesStrategy: no modificados.
Push pendiente por autenticación personal; sin reset/force-push/credenciales.
