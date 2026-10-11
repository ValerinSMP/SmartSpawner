# D5: capacidad negativa en GraalVM 25.0.3
Custom reporto READY con -3172 unidades y productive=-1 en bloque de stock25/cupo90.
No era solo texto. Reproduccion standalone con JAR production.2 y geometria/agrupaciones
de3354 registros, IDs de propietarios/granjas seudonimizados. GraalVM25.0.3 genera negativo
al calentarse; el mismo ensayo interpretado(-Xint), HotSpotC2(-UseJVMCICompiler) o excluyendo
solo spatialFree del compilador produce valores validos. Evidencia local, no afirmar issue
publicado por Oracle. No se modifico JVM/configuracion activa del servidor.
Correccion: sustituir min/max long anidados y estrechamiento por resta long/comparaciones
explicitas y salida cero cuando no queda capacidad. Grant rechaza negativos y capacidad
superior al stock. Geometria33/cap100 y permisos no cambian.
143 tests, cero fallos/errores, build personal limitado. Regresion con ventanas calentadas
y defensa de Grant. JAR FINAL verificado standalone con mismo GraalVM, seis cupos50..100,
3354 bloques: cero negativos, resultados iguales a interprete/C2. Cupos uniformes simulados,
no equivalen a capacidad real de Custom por rangos/contextos. Prueba con todas las flags
del servidor y heap reducido no arranco por SurvivorRatio incompatible: NO prueba pasada.
Instalacion Custom autorizada por usuario 2026-10-11T19:16:05Z. production.3 SHA256
df2d31ce7653e0cc10999f811c95d2d8aec11c101a7f62a1c12fe1db2709a03e.
Backup HDD /mnt/storage/backups/ai/survival-custom/smartspawner-custom-graal-fix-20261011T191605Z.
Incluye JAR/config y SQLite consistente con integrity_check=ok. Snapshot3354/23729.
Lectura SQL bajo usuario pterodactyl evita auxiliares WAL/SHM ajenos.
Config intacta, DB no restaurada/modificada. Ningun lifecycle, UAR ni reload.
Activo sigue production.2 hasta reinicio propietario. Pendiente verificar READY,
capacidad>=0, bloque productive dentro de0..stock, produccion real, TPS y guardados.
182legacy unresolved anteriores se conservan; no se inventan asignaciones.
Rollback: observation requiere reinicio propietario; restaurar JAR anterior devuelve bug
en este GraalVM. Preservar DB y sidecars, no restaurar backup de DB por rutina.

## Activacion verificada
Reinicio propietario19:20:20Z; production.3 enable16:21:23. Usuario16:23:21: READY/enforce/capacidad6918/plan156ms y bloque stock1/cupo50/productive1. Correccion de negativos confirmada en runtime. Snapshot3354/23729 intacto;3172asignados,182pendientes,0 nuevas asignaciones este arranque(no perdida de legado). Gameplay acumulacion de loot real todavia pendiente.
