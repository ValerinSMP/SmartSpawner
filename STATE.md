# SmartSpawner — D7 por tipo instalado, activación pendiente
2026-10-11. Fuente canónica personal: /root/development/projects/forks/upstream/SmartSpawner.
Rama work/valerin-production-contract-v1; versión 1.8.3-valerin-production.5.
Custom activo .3; .5 instalado en disco para reinicio diario del propietario. Sin lifecycle.
SHA256 0c191d7eef15dd137f867834d061540644e25b307a17c46fce4cb128f110e91f.
Backup original .3 HDD smartspawner-custom-overlap-fix-20261011T212446Z.
Backup final HDD smartspawner-custom-overlap-fix-20261011T212643Z (candidato intermedio .4).
Ambos snapshots SQLite consistentes/integrity ok:3182 bloques/18878 unidades al respaldo;
config intacta, ningún restore/SQL directo a stock. Los conteos son actuales, no snapshot inicial3354.

D7 decisión vigente: granja100/tipo y ventana33x33 cap100/tipo, entre jugadores y todas
las alturas. Corrige suma entre tipos de D3–D6. Stack50 continúa activo tras reload previo.
155tests/0fallos y shadowJar personal. Graal standalone3354filas/seisrangos sin negativos;
no gameplay. Backup HDD smartspawner-custom-per-type-20261011T215717Z (.4 previa), snapshot
3194bloques/19305unidades/integrityok,config intacta. Sin lifecycle en D7.
Entrega/migración/rollback vigente: docs/valerin/DELIVERY-D7-PER-TYPE-20261011.md.

D6 conservado: colocador UUID durable antes de protección; nuevo bloque cambia responsable del objeto
retirado al colocador. Apilado por otro responsable rechazado antes de consumo. Asignaciones
completas existentes no se reatribuyen. Raíces superpuestas/alias administrativos comparten
un presupuesto durable/transitivo100 por tipo, sin agrupar terrenos independientes por dueño.
Cupos por tipo50/60/70/80/90/100 y ventana continua33x33/cap100 se mantienen. Preserva stock,
loot/XP, callbacks, huevo<=50/venta previa, Mending/Eco y fix Graal .3.
152 pruebas unitarias y shadowJar aprobados en personal limitado. Simulación standalone
GraalVM25.0.3:3354 filas históricas/seisrangos uniformes sin negativos; no prueba gameplay.
Entrega/migración/rollback: docs/valerin/DELIVERY-D6-OVERLAP-PLACER-20261011.md.
Pendiente después del reinicio diario: log .5, READY, /ss mine colocador, generación y
pruebas de granja compartida/stack/Mending/huevo. No reiniciar/recargar por inferencia.
Test continúa .2; Sync no modificado. API reciclaje permanece DRAFT/sin proveedor runtime.

D5 .3 activo tras reinicio propietario19:20Z y READY6918 confirmado en juego.
Ticket solapamiento recuperado con comandos nativos:12bloques/38unidades misma granja;
producción real original confirmada. Drowned pausa manual preexistente preservada.
Detalles privados solo HomeSV staging/KYO-RECOVERY-20261011.md; no UUID/coords en Git.
DEVELOPMENT_HANDOFF.md ajeno intacto/untracked SHA256
4df0d596f2b4c23c983c4c8721cfa518bb78b5606038d4d9110af8e5e42dcb48.
Lecturas SQLite vivas siempre con usuario servidor pterodactyl/UID999 para no crear WAL
con dueño incorrecto. Rollback conserva datos posteriores; restaurar solo JAR .3 autorizado.

Ajuste autorizado posterior: Custom config spawner_properties.default.max_stack_size=50; /ss reload ejecutado correctamente 21:32:17Z, sin reinicio. Reload llama loadConfigurationValues para spawners actuales (incluye maxStackSize); el cambio de dueño/solapamientos .4 sigue pendiente del reinicio diario. Config SHA25618f3ac2d0d8c51e111d42e3df9dd2605110bba8470ef773ea32a03da15b7c07b. Backup HDD smartspawner-stack50-20261011T213217Z. Sin cambios de código/JAR ni SQL directo. Gameplay apilado50 pendiente.
