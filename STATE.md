# SmartSpawner — D6 instalado, activación pendiente
2026-10-11. Fuente canónica personal: /root/development/projects/forks/upstream/SmartSpawner.
Rama work/valerin-production-contract-v1; versión 1.8.3-valerin-production.4.
Custom activo .3; .4 instalado en disco para reinicio diario del propietario. Sin lifecycle.
SHA256 6b314e871359197389793b8b893c19a17ad6413eca432e7b16887971448b79f7.
Backup original .3 HDD smartspawner-custom-overlap-fix-20261011T212446Z.
Backup final HDD smartspawner-custom-overlap-fix-20261011T212643Z (candidato intermedio .4).
Ambos snapshots SQLite consistentes/integrity ok:3182 bloques/18878 unidades al respaldo;
config intacta, ningún restore/SQL directo a stock. Los conteos son actuales, no snapshot inicial3354.

D6: colocador UUID durable antes de protección; nuevo bloque cambia responsable del objeto
retirado al colocador. Apilado por otro responsable rechazado antes de consumo. Asignaciones
completas existentes no se reatribuyen. Raíces superpuestas/alias administrativos comparten
un presupuesto durable/transitivo100, sin agrupar terrenos independientes por dueño.
Cupos por tipo50/60/70/80/90/100 y ventana continua33x33/cap100 se mantienen. Preserva stock,
loot/XP, callbacks, huevo<=50/venta previa, Mending/Eco y fix Graal .3.
152 pruebas unitarias y shadowJar aprobados en personal limitado. Simulación standalone
GraalVM25.0.3:3354 filas históricas/seisrangos uniformes sin negativos; no prueba gameplay.
Entrega/migración/rollback: docs/valerin/DELIVERY-D6-OVERLAP-PLACER-20261011.md.
Pendiente después del reinicio diario: log .4, READY, /ss mine colocador, generación y
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
