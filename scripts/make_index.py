import json, sys, pathlib
d = pathlib.Path(sys.argv[1])
files = sorted(p.name for p in d.iterdir() if p.suffix.lower() == '.qst')
(d / 'index.json').write_text(json.dumps(files, ensure_ascii=False))
print('quêtes :', files)
