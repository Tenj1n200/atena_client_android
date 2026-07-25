import { useState } from 'react';
import TermuxExec from './plugins/Shell';

export default function App() {
  const [command, setCommand] = useState('echo "Olá do Termux" && ls');
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);

  async function handleRun() {
    if (!command.trim()) return;
    setLoading(true);
    setError(null);
    setResult(null);
    try {
      const res = await TermuxExec.execute({ command });
      setResult(res);
    } catch (e) {
      setError(e.message || String(e));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div style={styles.container}>
      <h1 style={styles.title}>Atena — Terminal</h1>
      <textarea
        style={styles.textarea}
        value={command}
        onChange={(e) => setCommand(e.target.value)}
        rows={3}
        spellCheck={false}
      />
      <button style={styles.button} onClick={handleRun} disabled={loading}>
        {loading ? 'Executando...' : 'Executar comando'}
      </button>

      {error && <div style={styles.errorBox}><strong>Erro:</strong> {error}</div>}

      {result && (
        <div style={styles.resultBox}>
          <div style={styles.resultHeader}>Exit code: {result.exitCode}</div>
          {result.stdout && (<><div style={styles.label}>stdout</div><pre style={styles.pre}>{result.stdout}</pre></>)}
          {result.stderr && (<><div style={{...styles.label, color:'#e05555'}}>stderr</div><pre style={{...styles.pre, color:'#e05555'}}>{result.stderr}</pre></>)}
        </div>
      )}
    </div>
  );
}

const styles = {
  container: { minHeight: '100vh', background: '#0f1115', color: '#e6e6e6', padding: 20, fontFamily: 'system-ui, sans-serif' },
  title: { fontSize: 20, marginBottom: 16 },
  textarea: { width: '100%', boxSizing: 'border-box', background: '#1a1d24', color: '#e6e6e6', border: '1px solid #2c313c', borderRadius: 8, padding: 12, fontFamily: 'monospace', fontSize: 14 },
  button: { marginTop: 12, width: '100%', padding: 14, background: '#3b82f6', color: '#fff', border: 'none', borderRadius: 8, fontSize: 16, fontWeight: 600 },
  errorBox: { marginTop: 16, padding: 12, background: '#3a1a1a', border: '1px solid #e05555', borderRadius: 8, color: '#ff9999' },
  resultBox: { marginTop: 16, padding: 12, background: '#1a1d24', border: '1px solid #2c313c', borderRadius: 8 },
  resultHeader: { fontSize: 13, color: '#8a93a6', marginBottom: 8 },
  label: { fontSize: 12, color: '#8a93a6', marginTop: 8, marginBottom: 4, textTransform: 'uppercase' },
  pre: { background: '#0b0d11', padding: 10, borderRadius: 6, fontSize: 13, overflowX: 'auto', whiteSpace: 'pre-wrap', margin: 0 },
};