# SmartSpawner — D4 Custom instalado
2026-10-11. Canonica personal /root/development/projects/forks/upstream/SmartSpawner.
Rama work/valerin-production-contract-v1. Version1.8.3-valerin-production.2.
Regla legado nueva aprobada usuario: ultimo actor registrado -> titular proteccion si falta.
Checkpoint SQL congela input por ID; asignacion/auditoria durable, no reatribuir por futuras
visitas ni sobrescribir transferencias. RPB raiz agrupa granjas; sin proteccion celda33x33
mas ventana CONTINUA33x33/cap100. Nuevo /ss mine paginado privado/defaulttrue/read-only.
Mantiene D3: egg50 y venta previa; tiers50/60/70/80/90/100, stock/loot/XP conservados.
SQL/LP en worker, snapshot protecciones main batches32/objetivo2ms sin cargar chunks.
141 tests automatizados y shadowJar personal limitado aprobados. Usuario confirma pruebas1-4 de gameplay Test.
Instalado SOLO Test f2ea450e-02a0-416c-9838-e73458d37b1c: enforce/migration=true/egg50.
SHA256427d06d9d3c1f19f19cc590b194cd5f35f6cd893690ed4e154f82fc7ea924747. Activo en Test confirmado por log production.2; reinicio realizado por propietario.
Backup consistente /mnt/storage/backups/ai/survival-custom/smartspawner-test-auto-20261011T183040Z.
Custom instalado autorizado por propietario: production.2 SHA256427d06d9d3c1f19f19cc590b194cd5f35f6cd893690ed4e154f82fc7ea924747. enforce/migration=true/egg50; activacion pendiente. Backup consistente HDD smartspawner-custom-production-20261011T184353Z. UAR now300 confirmado: Rework spawners, revisar Discord.
DEVELOPMENT_HANDOFF.md ajeno intacto sin seguimiento:
SHA2564df0d596f2b4c23c983c4c8721cfa518bb78b5606038d4d9110af8e5e42dcb48.
Contrato/prueba/rollback: docs/valerin/DELIVERY-D4-AUTOMIGRATION-MINE-20261011.md.
API reciclaje/procedencia/unidades sigue DRAFT; no autoridad cross-server runtime.
Siguiente: comprobar arranque Custom, migracion y MSPT/TPS. Propietario autoriza entrega y UAR300 en un solo reinicio; observacion previa descartada expresamente. Rollback: observation y reinicio autorizado, preservar DB/loot/XP posteriores.

Lectura actual Custom:3354 bloques,23729 unidades,424 actores distintos; cero last_actor vacios.
Test log confirma migracion3 bloques/unresolved0; no equivale a prueba de escala Custom.

Custom segundo reinicio propietario 18:56:55Z: SQLite recuperado tras permisos WAL/SHM y conexiones renovadas. Migracion3172 asignados/182 pendientes (log15:59:41); checkpoint3354. TPS reciente20.1/MSPT44-47, workers idle. Pendiente READY/cupos/gameplay y182 pendientes. Cierre anterior10 filas sucias fallaron SQLITE_READONLY: ultimos cambios no confirmados, sin restaurar DB. Lecturas SQLite futuras con UID servidor999/GID987.

D5 production.3 instalada Custom autorizada, sin lifecycle: corrige negativos reproducidos GraalVM25.0.3 espacial.143tests y simulacion3354/seisrangos sin negativos. SHA256df2d31ce7653e0cc10999f811c95d2d8aec11c101a7f62a1c12fe1db2709a03e. Backup HDD smartspawner-custom-graal-fix-20261011T191605Z; config intacta/SQLite snapshot3354/23729. Activo aun .2; propietario reinicia. Contrato docs/valerin/DELIVERY-D5-GRAAL-NEGATIVE-CAPACITY-20261011.md.
