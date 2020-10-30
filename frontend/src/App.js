import React, {useState} from 'react'
import Dropzone from 'react-dropzone'

function App() {
  const [state, setState] = useState({patch: "", results: null, error: false})

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
        setState({patch: patch, results: text, error: !response.ok})
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
  if (props.results == null) {
    return null;
  }
  if (props.error) {
    return <p>Error: {props.results}</p>
  }
  return (
    <ul>
      {Object.keys(props.results).map(file => {
        return <li key={file}>{file}: <FileResults fileResults={props.results[file]} /></li>
      })}
    </ul>
  );  
}

function FileResults(props) {
  if (props.fileResults.every(chunkResults => chunkResults.length === 0)) {
    return <em>no results found</em>
  }
  return (
    <ul>
      {props.fileResults.map((chunkResults, i) => <li key={"chunk-" + (i + 1)}>chunk #{i+1}: <ChunkResults chunkResults={chunkResults} /></li>)}
    </ul>
  );
}

function ChunkResults(props) {
  if (props.chunkResults.length === 0) {
    return <em>no results found</em>
  }
  return (
    <ul>
      {props.chunkResults.map((chunkResult, i) => <li key={"chunk-" + i}>{chunkResult.path} (score: {chunkResult.score.toFixed(0)})</li>)}
    </ul>
  );
}

export default App;
