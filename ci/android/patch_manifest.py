import sys
p = sys.argv[1]
m = open(p, encoding='utf-8').read()
r = open('ci/android/receivers.xml', encoding='utf-8').read()
if 'StreakWidget' not in m:
    m = m.replace('</application>', r + '\n</application>', 1)
open(p, 'w', encoding='utf-8').write(m)
