#!/usr/bin/env python3
"""
Code health report of simple-blog: BE test coverage and complexity (JaCoCo) and FE complexity (oxlint), as a static HTML page.

    ./code-health.py                  runs the BE tests with coverage, measures the FE, writes the report to ~/Downloads
    ./code-health.py --skip-tests     reuses the last JaCoCo report in be/build instead of running the tests again
    ./code-health.py --out DIR        writes DIR/simple-blog-code-health-<commit>/ (default ~/Downloads)
    ./code-health.py --previous DIR   compares with this earlier report folder (default: the newest one in the output directory)

The report folder holds index.html and jacoco/ (the full JaCoCo report). Only the Python standard library is used.
FE complexity is listed by running oxlint's `complexity` rule with max 0, which reports every function.
"""
import argparse, datetime, glob, html, json, os, re, shutil, subprocess, sys, tempfile
import xml.etree.ElementTree as ET

ROOT = os.path.dirname(os.path.abspath(__file__))
JAVA_HOME = '/opt/homebrew/opt/openjdk@25'
PREFIX = 'simple-blog-code-health-'

CSS = """:root{--bg:#f7f6f2;--card:#fff;--text:#22201c;--dim:#6f6a62;--line:#e4e0d8;--good:#2f8f4e;--warn:#c98a12;--bad:#c2412d;--track:#ece9e2;--accent:#2b5fa8}
@media (prefers-color-scheme:dark){:root:not([data-theme="light"]){--bg:#16171a;--card:#1f2125;--text:#e8e6e1;--dim:#9b978f;--line:#33363c;--good:#4cb871;--warn:#e0a93a;--bad:#e2664f;--track:#2c2f35;--accent:#7aa7e8}}
:root[data-theme="dark"]{--bg:#16171a;--card:#1f2125;--text:#e8e6e1;--dim:#9b978f;--line:#33363c;--good:#4cb871;--warn:#e0a93a;--bad:#e2664f;--track:#2c2f35;--accent:#7aa7e8}
*{box-sizing:border-box}body{margin:0;background:var(--bg);color:var(--text);font:15px/1.5 system-ui,-apple-system,"Segoe UI",Roboto,sans-serif}
main{max-width:1080px;margin:0 auto;padding:32px 16px 64px}h1{font-size:1.6rem;margin:0 0 4px}h2{font-size:1.1rem;margin:36px 0 10px}
.meta{color:var(--dim);font-size:.85rem}.mono{font-family:ui-monospace,Menlo,Consolas,monospace;font-size:.84rem;word-break:break-word}
.tiles{display:grid;grid-template-columns:repeat(auto-fit,minmax(180px,1fr));gap:12px;margin-top:20px}
.tile,.card{background:var(--card);border:1px solid var(--line);border-radius:10px}.tile{padding:14px 16px}
.tlabel{color:var(--dim);font-size:.8rem}.tvalue{font-size:1.7rem;font-weight:650;font-variant-numeric:tabular-nums}.tsub{color:var(--dim);font-size:.78rem}
.card{padding:4px 0;overflow-x:auto}table{width:100%;border-collapse:collapse;min-width:560px}th,td{padding:8px 14px;text-align:left;vertical-align:middle;border-bottom:1px solid var(--line)}
tr:last-child td{border-bottom:0}th{color:var(--dim);font-weight:500;font-size:.8rem}.r{text-align:right;font-variant-numeric:tabular-nums}
.bar{display:inline-block;width:90px;height:6px;background:var(--track);border-radius:3px;vertical-align:middle;margin-right:8px;overflow:hidden}
.bar span{display:block;height:100%}.good{background:var(--good)}.warn{background:var(--warn)}.bad{background:var(--bad)}
.up{color:var(--good);font-weight:600}.down{color:var(--bad);font-weight:600}.num{font-variant-numeric:tabular-nums;font-size:.85rem}.na,.dim{color:var(--dim);font-size:.78rem}
.pill{display:inline-block;min-width:28px;padding:1px 8px;border-radius:10px;text-align:center;font-weight:600;font-size:.82rem}
.cx-low{background:color-mix(in srgb,var(--good) 18%,transparent)}.cx-mid{background:color-mix(in srgb,var(--warn) 22%,transparent)}.cx-high{background:color-mix(in srgb,var(--bad) 25%,transparent)}
.two{display:grid;grid-template-columns:1fr 1fr;gap:16px}@media (max-width:760px){.two{grid-template-columns:1fr}}
.hist{padding:12px 16px}.hrow{display:flex;align-items:center;gap:10px;margin:6px 0}.hlab{width:44px;color:var(--dim);font-size:.82rem}
.hbar{flex:1;height:10px;background:var(--track);border-radius:5px;overflow:hidden}.hbar span{display:block;height:100%;background:var(--accent)}
.notes{padding:12px 20px}.notes li{margin:4px 0}a{color:var(--accent)}ul.plain{margin:0;padding:12px 32px}"""

e = html.escape


def run(cmd, cwd, env=None):
    return subprocess.run(cmd, cwd=cwd, env=env, capture_output=True, text=True)


def run_be_tests():
    env = dict(os.environ)
    env.setdefault('JAVA_HOME', JAVA_HOME)
    result = run(['./gradlew', 'coverage'], os.path.join(ROOT, 'be'), env)
    if result.returncode != 0:
        sys.exit('./gradlew coverage failed:\n' + (result.stdout + result.stderr)[-3000:])


def test_count():
    tests = failed = 0
    for path in glob.glob(os.path.join(ROOT, 'be/build/test-results/test/*.xml')):
        suite = ET.parse(path).getroot()
        tests += int(suite.get('tests'))
        failed += int(suite.get('failures')) + int(suite.get('errors'))
    return tests, failed


def counters(element):
    return {c.get('type'): (int(c.get('covered')), int(c.get('missed'))) for c in element.findall('counter')}


def rate(counter):
    covered, missed = counter
    return None if covered + missed == 0 else 100 * covered / (covered + missed)


def read_jacoco():
    report = ET.parse(os.path.join(ROOT, 'be/build/reports/jacoco/test/jacocoTestReport.xml')).getroot()
    packages, classes, methods = [], [], []
    for package in report.findall('package'):
        pname = package.get('name').replace('/', '.')
        packages.append((pname, counters(package)))
        for cls in package.findall('class'):
            cname = cls.get('name').split('/')[-1].replace('$', '.')
            classes.append((cname, pname, counters(cls)))
            for method in cls.findall('method'):
                methods.append(dict(cls=cname, pkg=pname, name=method_label(method.get('name')), line=method.get('line'), c=counters(method)))
    return counters(report), packages, classes, methods


def method_label(name):
    lam = re.match(r'lambda\$(\w+?)\$\d+', name)
    if lam:
        return 'lambda in ' + lam.group(1)
    return 'constructor' if name == '<init>' else name


def read_fe():
    config = tempfile.NamedTemporaryFile('w', suffix='.json', delete=False)
    json.dump({'plugins': ['react', 'typescript', 'oxc'], 'rules': {'eslint/complexity': ['warn', {'max': 0}]}}, config)
    config.close()
    try:
        result = run(['npx', 'oxlint', '-c', config.name, '-f', 'json', 'src'], os.path.join(ROOT, 'fe'))
    finally:
        os.unlink(config.name)
    functions = []
    for d in json.loads(result.stdout)['diagnostics']:
        value = int(re.search(r'complexity of (\d+)', d['message']).group(1))
        name = re.search(r'`([^`]+)`', d['message'])
        functions.append((value, name.group(1) if name else 'anonymous function', d['filename'].replace('src/', ''), d['labels'][0]['span']['line']))
    return functions


def git(*args):
    return subprocess.check_output(['git', *args], cwd=ROOT).decode().strip()


def previous_report(out_dir, current, explicit):
    """The folder to compare with and what it measured: read back from its 'Now' column and its commit line."""
    if explicit:
        folder = explicit
    else:
        found = [p for p in glob.glob(os.path.join(out_dir, PREFIX + '*')) if os.path.basename(p) != os.path.basename(current)]
        folder = max(found, key=os.path.getmtime) if found else None
    if not folder or not os.path.exists(os.path.join(folder, 'index.html')):
        return None, {}
    page = open(os.path.join(folder, 'index.html'), encoding='utf-8').read()
    commit = re.search(r'commit <span class="mono">(\w+)', page)
    values = {}
    for label, now in re.findall(r'<tr><td>([^<]+)</td><td class="r">[^<]*</td><td class="r">([^<]+)</td>', page):
        values[label] = float(now)
    return (commit.group(1) if commit else os.path.basename(folder)), values


def bar_class(percent):
    return 'good' if percent >= 90 else 'warn' if percent >= 70 else 'bad'


def bar(counter):
    percent = rate(counter)
    if percent is None:
        return '<span class="na">n/a</span>'
    return f'<div class="bar"><span class="{bar_class(percent)}" style="width:{percent:.1f}%"></span></div><span class="num">{percent:.1f}%</span>'


def pill(value):
    return f'<span class="pill cx-{"low" if value <= 5 else "mid" if value <= 10 else "high"}">{value}</span>'


def tile(label, value, sub):
    return f'<div class="tile"><div class="tlabel">{label}</div><div class="tvalue">{value}</div><div class="tsub">{sub}</div></div>'


def table(head, rows, right=()):
    ths = ''.join(f'<th class="r">{h}</th>' if i in right else f'<th>{h}</th>' for i, h in enumerate(head))
    return f'<div class="card"><table><thead><tr>{ths}</tr></thead><tbody>{"".join(rows)}</tbody></table></div>'


def histogram(values, label):
    buckets = [('1', lambda v: v == 1), ('2–3', lambda v: 2 <= v <= 3), ('4–5', lambda v: 4 <= v <= 5), ('6–10', lambda v: 6 <= v <= 10), ('11+', lambda v: v > 10)]
    rows = ''
    for name, test in buckets:
        n = len([v for v in values if test(v)])
        rows += f'<div class="hrow"><span class="hlab">{name}</span><div class="hbar"><span style="width:{100 * n / len(values):.1f}%"></span></div><span class="num">{n}</span></div>'
    return f'<div class="card hist"><div class="tlabel">{label} ({len(values)})</div>{rows}</div>'


def change_section(prev_commit, prev, now):
    if prev_commit is None:
        return ''
    rows = ''
    for label, value, good in now:
        if label not in prev:
            continue
        before = prev[label]
        fmt = (lambda x: f'{x:.2f}') if isinstance(value, float) and label.endswith(('%', 'complexity')) else (lambda x: str(int(x)))
        delta = value - before
        mark = '' if abs(delta) < 0.005 or good == 0 else ('up' if delta * good > 0 else 'down')
        text = fmt(0) if abs(delta) < 0.005 else f'{"+" if delta > 0 else "-"}{fmt(abs(delta))}'
        rows += f'<tr><td>{label}</td><td class="r">{fmt(before)}</td><td class="r">{fmt(value)}</td><td class="r {mark}">{text}</td></tr>'
    head = f'<th>Measure</th><th class="r">Previous ({e(prev_commit)})</th><th class="r">Now</th><th class="r">Change</th>'
    return f'<h2>Change since the previous report</h2><div class="card"><table><thead><tr>{head}</tr></thead><tbody>{rows}</tbody></table></div>'


def build(args):
    if not args.skip_tests:
        run_be_tests()
    total, packages, classes, methods = read_jacoco()
    tests, failed = test_count()
    fe = read_fe()
    line, branch, method = total['LINE'], total['BRANCH'], total['METHOD']
    be_cx = [sum(m['c'].get('COMPLEXITY', (0, 0))) for m in methods]
    be_avg, be_max = sum(be_cx) / len(be_cx), max(be_cx)
    fe_values = [f[0] for f in fe]
    fe_avg, fe_max, fe_over = sum(fe_values) / len(fe), max(fe_values), len([v for v in fe_values if v > 10])
    sha = git('rev-parse', '--short', 'HEAD')
    subject = git('log', '-1', '--format=%s')
    dirty = bool(git('status', '--porcelain'))
    out = os.path.join(os.path.expanduser(args.out), PREFIX + sha)
    prev_commit, prev = previous_report(os.path.expanduser(args.out), out, args.previous)
    now = [('Line coverage %', rate(line), 1), ('Branch coverage %', rate(branch), 1), ('Method coverage %', rate(method), 1),
           ('BE avg complexity', be_avg, -1), ('BE max complexity', be_max, -1), ('FE avg complexity', fe_avg, -1), ('FE max complexity', fe_max, -1),
           ('FE functions over 10', fe_over, -1), ('BE methods', len(methods), 0), ('FE functions', len(fe), 0)]
    fetcher = [c for n, _, c in classes if n == 'BookPageFetcher']
    lowest = sorted((rate(c['LINE']), n) for n, _, c in classes if rate(c.get('LINE', (0, 0))) is not None)[:1]
    findings = f"""<h2>Findings</h2><div class="card notes"><ul>
<li>The BE suite has {tests} passing tests{f' and <b>{failed} failing</b>' if failed else ''}. They cover {rate(line):.1f}% of lines and {rate(branch):.1f}% of branches.</li>
<li>The class with the lowest line coverage is <span class="mono">{lowest[0][1]}</span> ({lowest[0][0]:.0f}%).{' <span class="mono">BookPageFetcher</span> is at ' + format(rate(fetcher[0]['LINE']), '.0f') + '%: the controller tests mock it.' if fetcher and lowest[0][1] != 'BookPageFetcher' else ''}</li>
<li>Complexity: {len([v for v in be_cx if v > 10])} BE methods and {fe_over} FE functions exceed a cyclomatic complexity of 10. The highest BE method is <span class="mono">{max(methods, key=lambda m: sum(m['c'].get('COMPLEXITY', (0, 0))))['cls']}.{max(methods, key=lambda m: sum(m['c'].get('COMPLEXITY', (0, 0))))['name']}</span> ({be_max}); the FE maximum is {fe_max}.</li>
<li>The FE has no test runner, so it has no coverage figure. Its complexity comes from oxlint's <span class="mono">complexity</span> rule (run with max 0 to list every function) over <span class="mono">fe/src</span>: {len(fe)} functions.</li>
<li>BE coverage comes from JaCoCo over <span class="mono">be/src/main</span>, and BE complexity is JaCoCo's cyclomatic count per method. Lambdas count as their own methods. <a href="jacoco/index.html">The full JaCoCo report</a> lets you drill down to source lines.</li>
</ul></div>"""
    pkg_rows = [f'<tr><td class="mono">{n}</td><td>{bar(c.get("LINE", (0, 0)))}</td><td>{bar(c.get("BRANCH", (0, 0)))}</td><td class="r">{c.get("LINE", (0, 0))[0]} / {sum(c.get("LINE", (0, 0)))}</td><td class="r">{sum(c.get("COMPLEXITY", (0, 0)))}</td></tr>'
                for n, c in sorted(packages, key=lambda x: rate(x[1].get('LINE', (0, 0))) if rate(x[1].get('LINE', (0, 0))) is not None else 101)]
    unc = sorted([x for x in classes if x[2].get('LINE', (0, 0))[1] > 0 or x[2].get('BRANCH', (0, 0))[1] > 0], key=lambda x: -x[2]['INSTRUCTION'][1])
    cls_rows = [f'<tr><td class="mono">{n}<div class="dim">{p}</div></td><td>{bar(c.get("LINE", (0, 0)))}</td><td>{bar(c.get("BRANCH", (0, 0)))}</td><td class="r">{c.get("LINE", (0, 0))[1]}</td><td class="r">{c.get("BRANCH", (0, 0))[1]}</td></tr>' for n, p, c in unc]
    unreached = ''.join(f'<li class="mono">{e(m["cls"])}.{e(m["name"])} <span class="dim">({m["pkg"]}:{m["line"]})</span></li>' for m in methods if m['c'].get('METHOD', (1, 0))[0] == 0)
    top = sorted(methods, key=lambda m: -sum(m['c'].get('COMPLEXITY', (0, 0))))[:15]
    be_rows = [f'<tr><td class="mono">{e(m["cls"])}.{e(m["name"])}<div class="dim">{m["pkg"]}:{m["line"]}</div></td><td class="r">{pill(sum(m["c"].get("COMPLEXITY", (0, 0))))}</td><td>{bar(m["c"].get("LINE", (0, 0)))}</td><td>{bar(m["c"].get("BRANCH", (0, 0)))}</td></tr>' for m in top]
    fe_rows = [f'<tr><td class="mono">{e(n)}<div class="dim">{f}:{l}</div></td><td class="r">{pill(v)}</td></tr>' for v, n, f, l in sorted(fe, key=lambda x: -x[0])[:15]]
    meta = f'Branch <span class="mono">{e(git("rev-parse", "--abbrev-ref", "HEAD"))}</span> · commit <span class="mono">{sha} {e(subject)}</span>{" (plus uncommitted changes)" if dirty else ""} · generated {datetime.date.today()}'
    tiles = ''.join([tile('Line coverage', f'{rate(line):.1f}%', f'{line[0]} / {sum(line)} lines'), tile('Branch coverage', f'{rate(branch):.1f}%', f'{branch[0]} / {sum(branch)} branches'),
                     tile('Method coverage', f'{rate(method):.1f}%', f'{method[0]} / {sum(method)} methods'),
                     tile('BE complexity', f'{be_avg:.2f}', f'avg per method · max {be_max} · total {sum(be_cx)}'),
                     tile('FE complexity', f'{fe_avg:.2f}', f'avg per function · max {fe_max} · {len(fe)} functions')])
    page = f"""<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>simple-blog Code Health</title><style>{CSS}</style></head><body><main>
<h1>simple-blog code health</h1>
<div class="meta">{meta}</div>
<div class="tiles">{tiles}</div>
{change_section(prev_commit, prev, now)}
{findings}
<h2>BE coverage by package</h2>{table(['Package', 'Lines', 'Branches', 'Lines covered', 'Complexity'], pkg_rows, right=(3, 4))}
<h2>BE classes with uncovered code</h2>{table(['Class', 'Lines', 'Branches', 'Missed lines', 'Missed branches'], cls_rows, right=(3, 4))}
<h2>Methods no test reaches</h2><div class="card"><ul class="plain">{unreached}</ul></div>
<h2>Complexity distribution</h2><div class="two">{histogram(be_cx, 'BE methods')} {histogram(fe_values, 'FE functions')}</div>
<h2>Most complex BE methods</h2>{table(['Method', 'Complexity', 'Lines', 'Branches'], be_rows, right=(1,))}
<h2>Most complex FE functions</h2>{table(['Function', 'Complexity'], fe_rows, right=(1,))}
</main></body></html>"""
    shutil.rmtree(out, ignore_errors=True)
    os.makedirs(out)
    shutil.copytree(os.path.join(ROOT, 'be/build/reports/jacoco/test/html'), os.path.join(out, 'jacoco'))
    with open(os.path.join(out, 'index.html'), 'w', encoding='utf-8') as f:
        f.write(page)
    print(f'{out}/index.html  (compared with {prev_commit or "nothing"}; {tests} tests, {failed} failing)')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Writes the simple-blog code health report.')
    parser.add_argument('--skip-tests', action='store_true', help='reuse the last JaCoCo report instead of running the BE tests')
    parser.add_argument('--out', default='~/Downloads', help='directory for the report folder (default ~/Downloads)')
    parser.add_argument('--previous', help='report folder to compare with (default: the newest one in --out)')
    build(parser.parse_args())
