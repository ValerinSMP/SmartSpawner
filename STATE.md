# SmartSpawner — D1 propiedad/capacidad/API

Fecha: 2026-10-11. Fuente canónica para esta tarea: SSH personal,
 /root/development/projects/forks/upstream/SmartSpawner.
Rama work/valerin-production-contract-v1 sobre e12d173432aa73b841db077d12895ccff3facbe8.
Handoff DEVELOPMENT_HANDOFF.md preservado sin seguimiento, SHA-256
4df0d596f2b4c23c983c4c8721cfa518bb78b5606038d4d9110af8e5e42dcb48.

D1: contrato de propiedad/migración/API, API v1 DRAFT sin proveedor, planificador
offline y simulación agregada ro. Sin wiring de contabilidad runtime ni enforcement.
Política confirmada: default50/guardian60/ascendido70/arcano80/elite90/leyenda100
por tipo/responsable; permisos efectivos contextuales, no primary_group/OP.
Geometría/cifras espaciales/farm pendientes de aprobación; propuestas visibles.
Legado no tiene propiedad/procedencia certificadas; no atribución automática.

Archivos: api/.../production/v1/SpawnerUnitServiceV1.java, core/.../production/
ProductionSimulation.java, dos clases de regresión en core/src/test/.../production,
docs/valerin/{PRODUCTION-OWNERSHIP-API-V1,RECYCLER-HANDOFF-V1,DELIVERY-D1-20261011}.md,
docs/valerin/impact-20261011.json, tools/valerin/simulate-home-impact.py.
Mending/Eco, huevos/callbacks y código runtime existente sin modificaciones.
README actualizado con enlace al contrato interno; reglas AGENTS upstream conservadas.

Build con wrapper personal: :core:test :core:shadowJar :api:jar,
--no-daemon --no-parallel --max-workers=1. 102 tests aprobados, cero fallos/errors/skips.
16 nuevos tests de reglas puras/DTO; no prueba de LuckPerms vivo, SQL/migración,
autenticidad/escrow, caída durable, pagos o gameplay. Avisos Shadow heredados.
Impacto live: 3359 bloques/23722 unidades; stock persistido incluso unloaded.
Simulación de rangos usa sensibilidad histórica no propietarios certificados.
Backup fuentes+Git+handoff en disco secundario, ver docs/valerin/DELIVERY-D1-20261011.md.

Candidatos:
 /root/development/artifacts/SmartSpawner/1.8.3-production-contract-v1/
SmartSpawner-1.8.3-contract-v1.jar SHA-256
213db3e5bd7763e4e8f6c6155ae407c8d7c3bc4314d40a17687085e2d68cb8cf
SmartSpawner-api-1.8.3-contract-v1.jar SHA-256
f17c078ac5d7046849a9e8766c5422c512eda8c54d5e0a03bbf6a2d31e42c392

Instalado en Custom observado: JAR anterior hash
2308273a90cc5d9f1fac0c21dc9cd94e14e95fe4df0b85b97de07425b0ba16e2
bajo nombre SmartSpawner-1.6.6.jar. Sync sin JAR en volumen revisado.
Nuevo candidato NO instalado/activo. Sin lifecycle ni cambios en producción,
datos, loot, XP, permisos o esquema. Propietario realiza reinicios/recargas.
Siguiente incremento: contabilidad durable y modo de observación sin limitar,
después identidad/custodia/API, luego enforcement tras revisión espacial.
Push pendiente si continúa ausencia de autenticación HTTPS en personal.
