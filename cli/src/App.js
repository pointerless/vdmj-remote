import {ReactTerminal} from "react-terminal";
import BackendAPI from "./backend-api";
import {Tab, Tabs} from "react-bootstrap";
import {useEffect, useState} from "react";
import {CircleLoader} from "react-spinners";
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { icon } from '@fortawesome/fontawesome-svg-core/import.macro'
import './App.css';

function unravelIntoSpans(text, keyStart, delim='\n'){
    let arr = text.split(delim);
    return (
        <div>
            {
                (() => {
                    let container = [];
                    arr.forEach((val, index) => {
                        if(val !== null && val !== "") {
                            container.push(<span key={`${keyStart}-${index}`}> {val} <br/> </span>)
                        }
                    });
                    return container;
                })()
            }
        </div>
    );
}


const backendAPI = new BackendAPI();

function App() {
    const [startupMessage, setStartupMessage] = useState(null);
    const [outputs, setOutputs] = useState(null);
    const [runningOutputs, setRunningOutputs] = useState(new Map());

    const refreshAll = () => {
        backendAPI.getOutputs()
            .then(outputArr => {
                setOutputs(outputArr);
            }).catch(err => {console.error(err)})

        backendAPI.getStartup()
            .then(startup => {
                setStartupMessage(unravelIntoSpans(startup, "startup"));
            }).catch(err => {console.error(err)})
    }

    useEffect(() => {
        refreshAll();
    }, [])

    const welcome = (
        <div>
            <span>VDM Web Terminal (use <b>clear</b> to remove output)<br/> </span>
            { startupMessage }
        </div>
    );

    const commandHandler = async (command, args) => {
        return new Promise((resolve) => {
            backendAPI.execute(`${command} ${args}`)
                .then(o => resolve(unravelIntoSpans(o, command)))
                .catch(reason => resolve((<p style={{color: "darkred"}}>{reason}</p>)))
        })
    }

    const startOutput = async (output) => {
        if(runningOutputs.has(output.id)){
            await backendAPI.stopOutput(output);
        }
        await backendAPI.startOutput(output)
            .then(info => {
                setRunningOutputs(new Map(runningOutputs.set(output.id, info)));
                console.log(runningOutputs);
            }).catch(err => {
                console.error(err);
            });
    }

    if(startupMessage === null && outputs === null){
        return (
            <div className="App">
                <CircleLoader loading={true}>
                </CircleLoader>
            </div>
        )
    }

    const handleTabChange = async (eventKey) => {
        console.log(eventKey);
        if(eventKey === "terminal") return;
        if(runningOutputs.has(eventKey)) return;
        for(let output of outputs){
            if(output.id === eventKey){
                await startOutput(output);
                return;
            }
        }
        console.error(`Output not found: ${JSON.stringify(eventKey)}`)
    }

    // TODO: Add reload button on output tabs

    return (
        <Tabs defaultActiveKey="terminal" id="outputTabs" className="mb-3" onSelect={handleTabChange}>
            <Tab eventKey="terminal" title="Terminal">
                <div className="App" style={{resize: "vertical", overflow: "hidden",     border: "1px solid"}}>
                    <ReactTerminal
                        defaultHandler={commandHandler}
                        theme="material-dark"
                        welcomeMessage={welcome}
                        showControlBar={false}
                        prompt=">"
                    />
                </div>
            </Tab>
            {
                (() => {
                    let container = [];
                    if (outputs === null) return;
                    outputs.forEach((output, index) => {
                        if (output !== null && output !== "") {
                            if (runningOutputs.has(output.id)) {
                                container.push(
                                    <Tab eventKey={output.id}
                                         title={
                                             <>
                                                 {output.displayName}
                                                 <FontAwesomeIcon className="reload-button" onClick={
                                                     async (e) => {
                                                         await startOutput(output);
                                                         setTimeout(() => {
                                                             let iframe = document.getElementById(output.id);
                                                             iframe.src += '';}, 100);
                                                     }
                                                 } icon={icon({name: "rotate-right", style: "solid"})}/>
                                             </>
                                         }>
                                        <iframe id={output.id} title={output.id}
                                                src={runningOutputs.get(output.id).accessURL}
                                                style={{
                                                    width: "100%",
                                                    position: "relative",
                                                    overflow: "hidden scroll"
                                                }}
                                                scrolling="no" sandbox="allow-same-origin allow-scripts"
                                                allow="cross-origin-isolated">
                                        </iframe>
                                    </Tab>
                                )
                                setTimeout(() => {
                                    let iframe = document.getElementById(output.id);
                                    iframe.src += '';
                                }, 100);
                                window.addEventListener("message", (e) => {
                                    let thisFrame = document.getElementById(output.id);
                                    if (thisFrame === null) {
                                        console.log(`NULLED: ${output.id}`);
                                    } else if (thisFrame.contentWindow === e.source) {
                                        thisFrame.height = e.data.height + "px";
                                        thisFrame.style.height = e.data.height + "px";
                                    }
                                })
                            } else {
                                container.push(
                                    <Tab eventKey={output.id}
                                         title={output.displayName}>
                                        <CircleLoader></CircleLoader>
                                    </Tab>
                                )
                            }

                        }
                    })
                    return container;
                })()
            }
        </Tabs>
    );

}

export default App;
