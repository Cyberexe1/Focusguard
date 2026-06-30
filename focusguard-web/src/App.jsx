import { useEffect } from 'react'

// ── Where the APK lives. Drop your built APK at focusguard-web/public/focusguard.apk
//    or replace this with a direct S3 / GitHub Releases download link.
const APK_URL = '/focusguard.apk'
const APP_VERSION = 'v3.0.0'
const APK_SIZE = '18 MB'

const features = [
  { ic: 'i1', emoji: '🧠', title: 'AI Task Triage', desc: 'Drop a task in plain English. Nova-Lite parses the deadline, effort and category, then scores its priority for you.' },
  { ic: 'i2', emoji: '⏱️', title: 'Live Deadline Timers', desc: 'A stopwatch carousel counts down every deadline to the second, turning red the moment a task gets urgent.' },
  { ic: 'i3', emoji: '📅', title: 'Smart Day Timeline', desc: 'Build timed blocks on an hour-rail timeline and get a phone notification the instant each block begins.' },
  { ic: 'i4', emoji: '📞', title: 'Accountability Calls', desc: 'When risk spikes, FocusGuard rings your phone with a real voice call so you cannot ghost your own deadline.' },
  { ic: 'i5', emoji: '🔥', title: 'Habit Insights', desc: 'See your peak productive hours, consistency score and where you keep underestimating effort.' },
  { ic: 'i6', emoji: '🛡️', title: 'Risk Radar', desc: 'Every task gets a live risk score so you always know what is about to slip before it actually does.' },
]

const steps = [
  { n: '1', title: 'Download & sign up', desc: 'Install the APK, create your account in seconds, and add your phone for accountability calls.' },
  { n: '2', title: 'Add your deadlines', desc: 'Type tasks naturally. The AI ranks them and builds your focus timeline automatically.' },
  { n: '3', title: 'Ship on time', desc: 'Get timed reminders, risk alerts and voice nudges until everything is done.' },
]

function useReveal() {
  useEffect(() => {
    const els = document.querySelectorAll('.reveal')
    const obs = new IntersectionObserver(
      (entries) => entries.forEach((e) => e.isIntersecting && e.target.classList.add('in')),
      { threshold: 0.12 }
    )
    els.forEach((el) => obs.observe(el))
    return () => obs.disconnect()
  }, [])
}

export default function App() {
  useReveal()

  return (
    <>
      <div className="blob b1" />
      <div className="blob b2" />
      <div className="blob b3" />

      {/* Nav */}
      <nav className="nav">
        <div className="brand">
          <div className="logo">🛡️</div>
          FocusGuard <span style={{ color: 'var(--primary)' }}>AI</span>
        </div>
        <div className="links">
          <a href="#features">Features</a>
          <a href="#how">How it works</a>
          <a href="#download">Download</a>
        </div>
        <a href={APK_URL} download className="btn nav-cta">Get the app</a>
      </nav>

      {/* Hero */}
      <header className="wrap hero">
        <div className="reveal">
          <div className="pill"><span className="dot" /> Now live · {APP_VERSION}</div>
          <h1>Never miss a<br /><span className="grad">deadline</span> again.</h1>
          <p className="lead">
            FocusGuard AI is your pocket accountability coach. It triages your tasks,
            counts down every deadline, and calls your phone when you are about to slip.
          </p>
          <div className="cta-row">
            <a href={APK_URL} download className="btn">
              <span style={{ fontSize: 22 }}>⬇️</span>
              <span>
                <span className="sub">Download for</span>
                <span className="big">Android (APK)</span>
              </span>
            </a>
            <a href="#features" className="btn ghost">See features</a>
          </div>
          <div className="trust">
            <div className="stat"><div className="n">AI-ranked</div><div className="l">task priority</div></div>
            <div className="stat"><div className="n">Real calls</div><div className="l">when you slip</div></div>
            <div className="stat"><div className="n">{APK_SIZE}</div><div className="l">tiny install</div></div>
          </div>
        </div>

        {/* Phone mockup */}
        <div className="phone-stage reveal">
          <div className="phone">
            <div className="notch" />
            <div className="screen">
              <div className="scr-head">
                <div>
                  <div className="hi">Good evening,</div>
                  <div className="name">Vikas 👋</div>
                </div>
                <div className="av" />
              </div>

              <div className="glass-card timer">
                <div className="lbl">⏳ Hackathon MVP — time left</div>
                <div className="units">
                  <div className="u"><b>00</b><span>DAYS</span></div>
                  <div className="u"><b>06</b><span>HRS</span></div>
                  <div className="u"><b>42</b><span>MIN</span></div>
                  <div className="u"><b>18</b><span>SEC</span></div>
                </div>
              </div>

              <div className="glass-card task">
                <div className="bar" />
                <div>
                  <div className="t">Submit hackathon</div>
                  <div className="s">Today · 2:00 PM</div>
                </div>
                <div className="score">94</div>
              </div>
              <div className="glass-card task">
                <div className="bar" style={{ background: 'linear-gradient(#a855f7,#c4b5fd)' }} />
                <div>
                  <div className="t">DBMS assignment</div>
                  <div className="s">Tomorrow · 11:00 PM</div>
                </div>
                <div className="score" style={{ background: 'linear-gradient(135deg,#a855f7,#6d28d9)' }}>72</div>
              </div>
              <div className="glass-card task">
                <div className="bar" style={{ background: 'linear-gradient(#8b5cf6,#c4b5fd)' }} />
                <div>
                  <div className="t">Interview prep</div>
                  <div className="s">Thu · 10:00 AM</div>
                </div>
                <div className="score" style={{ background: 'linear-gradient(135deg,#8b5cf6,#c4b5fd)' }}>45</div>
              </div>
            </div>
          </div>
        </div>
      </header>

      {/* Features */}
      <section id="features" className="section wrap">
        <h2 className="reveal">Everything you need to <span style={{ color: 'var(--primary)' }}>stay on track</span></h2>
        <p className="sub reveal">Built around one promise: your deadlines stop being a surprise.</p>
        <div className="grid">
          {features.map((f, i) => (
            <div className="clay-card reveal" key={i} style={{ transitionDelay: `${i * 60}ms` }}>
              <div className={`ic ${f.ic}`}>{f.emoji}</div>
              <h3>{f.title}</h3>
              <p>{f.desc}</p>
            </div>
          ))}
        </div>
      </section>

      {/* How it works */}
      <section id="how" className="section wrap">
        <h2 className="reveal">Up and running in <span style={{ color: 'var(--secondary)' }}>3 steps</span></h2>
        <p className="sub reveal">No setup headaches. Install, add tasks, ship.</p>
        <div className="steps">
          {steps.map((s, i) => (
            <div className="step reveal" key={i} style={{ transitionDelay: `${i * 80}ms` }}>
              <div className="num">{s.n}</div>
              <h3>{s.title}</h3>
              <p>{s.desc}</p>
            </div>
          ))}
        </div>
      </section>

      {/* Download CTA */}
      <section id="download" className="wrap">
        <div className="download reveal">
          <h2>Ready to beat procrastination?</h2>
          <p>Download FocusGuard AI and let it guard your deadlines for you.</p>
          <a href={APK_URL} download className="btn">⬇️ Download APK · {APP_VERSION}</a>
          <div className="meta">Android 8.0+ · {APK_SIZE} · Free</div>
        </div>
      </section>

      {/* Footer */}
      <footer className="footer wrap">
        <div className="brand"><span>🛡️</span> FocusGuard AI</div>
        <div>Your AI-powered deadline companion.</div>
        <div style={{ marginTop: 10 }}>© {new Date().getFullYear()} FocusGuard AI · Built for shippers.</div>
      </footer>
    </>
  )
}
