import React, {useState} from 'react'
import Dropzone from 'react-dropzone'

function App() {
  const [state, setState] = useState({patch: "", patchTargetResults: null, byContentResults: null, error: null})

  const onChange = event => {
    setState({patch: event.target.value, patchTargetResults: state.patchTargetResults, appliedPatchResults: state.appliedPatchResults, byContentResults: state.byContentResults, error: state.error})
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
      const patchTargetResponse = await fetch("/search", { method: "POST", body: patch })
      const patchTargetText = await patchTargetResponse.text()

      if (!patchTargetResponse.ok) {
        setState({patch: patch, patchTargetResults: null, appliedPatchResults: null, byContentResults: null, error: patchTargetText})
        return;
      }

      const appliedPatchResponse = await fetch("/search?applied_patch=true", { method: "POST", body: patch })
      const appliedPatchText = await appliedPatchResponse.text()
      if (!appliedPatchResponse.ok) {
        setState({patch: patch, patchTargetResults: null, appliedPatchResults: null, byContentResults: null, error: appliedPatchText})
      }

      const byContentResponse = await fetch("/search?by_content=true", { method: "POST", body: patch })
      const byContentText = await byContentResponse.text()
      if (!byContentResponse.ok) {
        setState({patch: patch, patchTargetResults: null, appliedPatchResults: null, byContentResults: null, error: byContentText})
        return;
      }

      setState({patch: patch, patchTargetResults: JSON.parse(patchTargetText), appliedPatchResults: JSON.parse(appliedPatchText), byContentResults: JSON.parse(byContentText), error: null})
    } catch (error) {
      setState({patch: patch, patchTargetResults: null, byContentResults: null, error: error.message})
    }
  }
  
  return (
    <div className="App">
      <header>
        <img src="chameleon.svg" alt=""/>
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
          <ResultBox error={state.error} patchTargetResults={state.patchTargetResults} appliedPatchResults={state.appliedPatchResults} byContentResults={state.byContentResults} />
        </form>
      </main>
    </div>
  );
}

function ResultBox(props) {
  if (props.error != null) {
    return <p>Error: {props.error}</p>
  }
  if (props.patchTargetResults == null && props.byContentResults == null && props.appliedPatchResults == null) {
    return null;
  }
  return (
    <div>
        <h2>Potential patch targets</h2>
        <ul className="level-1">
          {Object.keys(props.patchTargetResults).map(file => {
            return <li className="level-1-element" key={file}>{file}: <FileResults fileResults={props.patchTargetResults[file]} /></li>
          })}
        </ul>

        <h2>Potentially already fixed code</h2>
        <ul className="level-1">
          {Object.keys(props.appliedPatchResults).map(file => {
            return <li className="level-1-element" key={file}>{file}: <FileResults fileResults={props.appliedPatchResults[file]} /></li>
          })}
        </ul>

        <h2>Potential patch copies</h2>
        <FileResults fileResults={props.byContentResults} />
    </div>
  );
}

function FileResults(props) {
  if (props.fileResults.length === 0) {
    return <em>no results found</em>
  }
  return (
    <ul className="level-2">
      {props.fileResults.map(result => <li className="level-2-element" key={"file-" + result}>{result.path} (score: {result.score.toFixed(0)})</li>)}
    </ul>
  );
}

export default App;
