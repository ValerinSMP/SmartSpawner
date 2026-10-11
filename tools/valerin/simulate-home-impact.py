"""Read-only aggregate reference simulation. Never exports identities or coordinates."""
import collections, datetime, hashlib, json, sqlite3
from pathlib import Path
BASE=Path('/home/mtyn/staging/survival-custom/spawner-economy-20261011')
OUT=Path('/home/mtyn/staging/survival-custom/smartspawner-production-design-20261011')
DB='/var/lib/pterodactyl/volumes/9292cb5d-ec3b-4833-a0c3-52aff6c35045/plugins/SmartSpawner/spawners.db'
c=sqlite3.connect('file:'+DB+'?mode=ro',uri=True)
c.execute('pragma query_only=on');c.execute('begin')
rows=c.execute('select world,loc_x,loc_y,loc_z,stack_size from sspawner_data').fetchall()
c.rollback();c.close()
valid=[r for r in rows if r[4]>0 and r[0] is not None]
chunks=collections.Counter()
worlds=collections.defaultdict(list)
for world,x,y,z,n in valid:
 chunks[world,x//16,z//16]+=n;worlds[world].append((x,z,n))
spatial=[]
for span in (32,48):
 counts=collections.Counter();max_units=0;windows=0
 for positions in worlds.values():
  for left in sorted({p[0] for p in positions}):
   zs=collections.Counter()
   for x,z,n in positions:
    if left<=x<=left+span:zs[z]+=n
   band=sorted(zs.items());j=0;total=0
   for i,(bottom,_) in enumerate(band):
    while j<len(band) and band[j][0]<=bottom+span:
     total+=band[j][1];j+=1
    windows+=1;max_units=max(max_units,total)
    for cap in (100,150,200):
     if total>cap:counts[cap]+=1
    total-=band[i][1]
 for cap in (100,150,200):
  spatial.append(dict(span=span,integer_side=span+1,cap=cap,candidate_windows=windows,
   overloaded_candidate_windows=counts[cap],maximum_window_stock=max_units,
   minimum_hold_required_in_densest_window=max(0,max_units-cap)))
old=json.loads((BASE/'scenario-50-100.json').read_text())
# The old affected set is sufficient for monotone increases from the old 50/100 scenario.
# These are aggregate bounds, not certified ownership or live effective permissions.
owner_sensitivity=[]
for base in (50,60,70,80,90,100):
 excess=0;affected=0
 for owner in old['affected']:
  cap=100 if owner['leyenda_inherited'] else base
  e=sum(max(0,t['units']-cap) for t in owner['over_by_type'].values())
  excess+=e;affected+=e>0
 owner_sensitivity.append(dict(non_legend_scenario_cap=base,legend_cap=100,estimated_hold=excess,affected_protection_holders=affected))
result=dict(snapshot_utc=datetime.datetime.now(datetime.timezone.utc).isoformat(),
 method='Single read-only SQLite transaction including WAL; all durable placements, irrespective of loaded chunks or activators.',
 live_total_blocks=len(rows),live_total_units=sum(r[4] for r in valid),invalid_rows=len(rows)-len(valid),
 ownership='No durable owner exists; property-holder attribution is not ownership certification.',
 rank_policy=dict(default=50,guardian=60,ascendido=70,arcano=80,elite=90,leyenda=100),
 historical_owner_snapshot=old['snapshot_utc'],historical_unattributed_units=old['unattributed_units'],
 historical_permission_context_pending=old['rank_context_pending_owners'],
 historical_sensitivity=owner_sensitivity,
 spatial_proposals=spatial,
 chunk_comparison=[dict(cap=cap,chunks_over=sum(n>cap for n in chunks.values()),
 excess_units=sum(max(0,n-cap) for n in chunks.values())) for cap in (50,100,150,200)],
 caveats=['Spatial figures and rank sensitivity overlap; never add their excesses.',
 'Overloaded windows overlap. Counts are not farms or people and their excesses cannot be summed.',
 'Windows are occupied-coordinate extremal candidates; exact maximum, not a count of every possible geometric window.',
 'No actual production/income measured; live and historical snapshots differ.',
 'Rank sensitivity is a range over old protection-holder attribution, not a simulation with new effective permissions.',
 'Neither production nor inventory, loot, XP, SQL schema, permissions or servers were modified.'])
OUT.mkdir(parents=True,exist_ok=True)
(OUT/'impact.json').write_text(json.dumps(result,indent=2)+'\n')
print(json.dumps(result,indent=2))
