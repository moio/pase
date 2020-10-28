import React, {useState} from 'react'
import Dropzone from 'react-dropzone'

function App() {
  const [state, setState] = useState({patch: "", results: [], error: false})

  const onChange = event => {
    setState({patch: event.target.value, results: state.results, error: state.error})
  }

  const onDrop = async acceptedFiles => {
    const patch = await new Response(acceptedFiles[0]).text()
    search(patch)
  }

  const onPaste = async e => {
    const clipboardData = e.clipboardData || window.clipboardData
    const patch = clipboardData.getData('Text')
    search(patch)
  }

  const search = async (patch) => {
    try {
      const response = await fetch("/search", { method: "POST", body: patch })
      const text = await response.text()
      if (response.ok) {
        setState({patch: patch, results: JSON.parse(text), error: false})
      }
      else {
        setState({patch: patch, results: [text], error: !response.ok})
      }
    } catch (error) {
      setState({patch: patch, results: error.message, error: true})
    }
  }
  
  return (
    <div className="App">
      <header>
        <h1>PaSe - patch search</h1>
      </header>
      <main className="App-main">
        <form>
        <Dropzone onDrop={onDrop} noClick={true}>
            {({getRootProps, getInputProps}) => (
              <section>
                <div {...getRootProps()}>
                  <input {...getInputProps()} />
                  <textarea value={state.patch} rows={20} cols={80} placeholder="Paste or drop patch here..." onChange={onChange} onPaste={onPaste} />
                </div>
              </section>
            )}
          </Dropzone>
        </form>
        <ResultBox error={state.error} results={state.results} />
      </main>
    </div>
  );
}

function ResultBox(props) {
  if (props.error) {
    return <p>{props.results}</p>
  }
  if (props.results == null) {
    return;
  }
  if (props.results !== 0) {
    return <p>No results found.</p>
  }
  return (
    <ul>
      {props.results.map((r, i) => {
        return <li key={i}>{r.path}</li>
      })}
    </ul>
  );  
}

export default App;
