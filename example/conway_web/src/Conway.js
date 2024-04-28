import React, {useCallback, useEffect, useRef, useState} from "react";
import Button from 'react-bootstrap/Button';
import BackendAPI from './backend-api';
import {Container, Row, Col, Badge, Alert} from "react-bootstrap";
import {
    Chart as ChartJS,
    CategoryScale,
    LinearScale,
    PointElement,
    LineElement,
    Title,
    Tooltip,
    Legend,
} from 'chart.js';
import { Line } from 'react-chartjs-2';

ChartJS.register(
    CategoryScale,
    LinearScale,
    PointElement,
    LineElement,
    Title,
    Tooltip,
    Legend
);



function Conway() {
    let backendAPI = new BackendAPI();

    const [configuration, setConfiguration] = useState({configObject: { sideLength: null, sleepTime: null, pop: null}});
    const [times, setTimes] = useState({reqTime: 0, parseTime: 0, displayTime: 0})
    const [drawing, setDrawing] = useState(false);
    const [autoGenerating, setAutoGenerating] = useState(false);
    const [generationMinTime, setGenerationMinTime] = useState(1000);
    const [autoTimeoutNext, setAutoTimeoutNext] = useState(0);

    const convertConfiguration = (configString) => {
        configString = configString.match(/mk_Configuration\(.*\{.*}\)/gm)[0];
        let configObject = configString.replaceAll("{", '"pop": [')
        configObject = configObject.replaceAll("})", ']}')
        configObject = configObject.replaceAll(/mk_Point\((-?\d+),\s(-?\d+)\)/g, (s, c1, c2) => `[${c1}, ${c2}]`)
        configObject = configObject.replaceAll(/mk_Configuration\((\d+),\s?(\d+)/g, (s, c1, c2) => `{ "sideLength":${c1}, "sleepTime":${c2}`);
        let generationString = configString.replaceAll(/mk_Configuration\(\d+,\s?\d+, (.*)\).*/g, (s, c1) => {
            return c1
        })
        return {configObject: JSON.parse(configObject), generationString};
    }

    const convertGenerationString = (generationString) => {
        generationString = generationString.match(/\{.*}/gm)[0];
        return generationString.replaceAll(/mk_Configuration\(\d+,\s?\d+, (.*)\).*/g, (s, c1) => {
            return c1
        })
    }

    const convertPopulation = (generationString) => {
        generationString = generationString.match(/\{.*}/gm)[0];
        let popObject = generationString.replaceAll("{", "[")
        popObject = popObject.replaceAll("}", "]")
        popObject = popObject.replaceAll(/mk_Point\((-?\d+),\s(-?\d+)\)/g, (s, c1, c2) => `[${c1}, ${c2}]`)
        return JSON.parse(popObject);
    }

    const convertPopToString = (pop) => {
        let points = [];
        pop.forEach(point => {
            points.push(`mk_Point(${point[0]}, ${point[1]})`);
        })
        let s = "{"
        for(let i =0; i<points.length-1; i++){
            s += `${points[i]}, `
        }
        s += `${points[points.length-1]}}`;
        return s;
    }

    const handleNewGeneration = async () => {
        await backendAPI.newGeneration("GOSPER_GLIDER_GUN")
            .then(result => {
                let c = convertConfiguration(result);
                setConfiguration(c);
            })
    }

    const handleNextGeneration = async () => {
        if(configuration.configObject.pop === null){
            await backendAPI.newGeneration("GOSPER_GLIDER_GUN")
                .then(result => {
                    let c = convertConfiguration(result);
                    setConfiguration(c);
                })
        }else{
            await backendAPI.nextGeneration(configuration)
                .then(result => {
                    let tempConfig = {configObject: {}};
                    tempConfig.configObject.sideLength = configuration.configObject.sideLength;
                    tempConfig.configObject.sleepTime = configuration.configObject.sleepTime;
                    tempConfig.generationString = convertGenerationString(result);
                    tempConfig.configObject.pop = convertPopulation(result);
                    setConfiguration(tempConfig);
                })
        }
    };

    useEffect(() => {
        if(autoGenerating){
            if(Date.now() < autoTimeoutNext){
                setTimeout(() => {
                    setAutoTimeoutNext(0);
                }, Date.now() - autoTimeoutNext);
            }else{
                let start = Date.now();
                handleNextGeneration().then();
                let end = Date.now();
                setAutoTimeoutNext(Date.now() + generationMinTime-(end-start));
            }
        }
    }, [autoTimeoutNext, autoGenerating, configuration, generationMinTime])

    const handleStartStopAuto = async () => {
        if(autoGenerating){
            setAutoGenerating(false);
            return;
        }
        setAutoGenerating(true);
    }

    const handleStartStopDraw = () => {
        if(drawing){
            setDrawing(false);
            return;
        }
        setDrawing(true);
    }

    const handleDrawClick = (event) => {
        if(!drawing) return;
        console.log("DRAWING");
        const rect = event.target.getBoundingClientRect();
        const x = event.clientX - rect.left;
        const y = event.clientY - rect.top;
        console.log({x, y});
        const cellSizeX = event.target.width/configuration.configObject.sideLength;
        const cellSizeY = event.target.height/configuration.configObject.sideLength;
        let p = [0, 0];
        p[0] = Math.floor((x-0.5-(event.target.width/2))/cellSizeX);
        p[1] = Math.ceil((y-0.5-(event.target.height/2))/-cellSizeY);
        console.log(p);

        let tempConfig = {configObject: {}};
        tempConfig.configObject.sideLength = configuration.configObject.sideLength;
        tempConfig.configObject.sleepTime = configuration.configObject.sleepTime;
        tempConfig.configObject.pop = configuration.configObject.pop;

        let wasAlive = false;
        tempConfig.configObject.pop = tempConfig.configObject.pop.filter(aliveCell => {
            if(aliveCell[0] === p[0] && aliveCell[1] === p[1]){
                wasAlive = true;
                return false;
            }
            return true;
        })

        if(!wasAlive){
            tempConfig.configObject.pop.push(p);
        }

        console.log(tempConfig);

        tempConfig.generationString = convertPopToString(tempConfig.configObject.pop);
        setConfiguration(tempConfig);
    }

    /*useEffect( () => {
        const nextGen = async () => {
            if(autoGenerating){
                console.log("Called");
                let start = Date.now();
                await handleNextGeneration();
                let end = Date.now();
                setTimeout(nextGen, (end-start < generationMinTime ? generationMinTime-(end-start) : 1))
            }
        }
        setTimeout(nextGen, 10);
    }, [autoGenerating, generationMinTime, handleNextGeneration])*/

    useEffect(() => {
        let start = Date.now();
        const canvas = document.getElementById("conway-output");
        if(canvas === null || configuration.configObject.pop === null){
            return;
        }

        const ctx = canvas.getContext("2d");
        ctx.reset();
        ctx.strokeStyle = "green";
        ctx.fillStyle = "white";
        ctx.lineWidth = 2;
        ctx.translate(canvas.width/2, canvas.height/2);

        let cellSizeX = canvas.width/configuration.configObject.sideLength;
        let cellSizeY = canvas.height/configuration.configObject.sideLength;

        for (let i = -configuration.configObject.sideLength/2; i <= configuration.configObject.sideLength/2; i++) {
            for (let j = -20; j <= 20; j++){
                const x = i*cellSizeX;
                const y = j*cellSizeY;
                ctx.strokeRect(x, y, cellSizeX, cellSizeY);
            }
        }

        let tempConfig = {configObject: {}};
        tempConfig.configObject.sideLength = configuration.configObject.sideLength;
        tempConfig.configObject.sleepTime = configuration.configObject.sleepTime;
        tempConfig.configObject.pop = configuration.configObject.pop;
        tempConfig.generationString = convertPopToString(configuration.configObject.pop.filter(p => {
            ctx.fillStyle = "black";
            if(Math.abs(p[0]) > configuration.configObject.sideLength+5 || Math.abs(p[1]) > configuration.configObject.sideLength+5){
                return false;
            }
            const x = p[0]*cellSizeX+0.5;
            const y = p[1]*-cellSizeY+0.5;
            ctx.fillRect(x, y, cellSizeX-1, cellSizeY-1);
            return true;
        }));
        let end = Date.now();
        setConfiguration(tempConfig);
        setTimes({reqTime: times.reqTime, parseTime: times.parseTime, displayTime: end - start})
    }, [configuration.configObject.pop])


    return (
        <Container fluid>
            <Row style={{padding: "5px"}}>
                <Col>
                    <Button id="next-generation" variant="primary" type="button" onClick={handleNextGeneration}
                            disabled={drawing || autoGenerating}>
                        Next Generation
                    </Button>
                    <Button id="new-generation" variant="secondary" type="button" onClick={handleNewGeneration}
                            disabled={drawing || autoGenerating}>
                        New Generation
                    </Button>
                    <Button id="auto-generate" variant="secondary" type="button" onClick={handleStartStopAuto}
                        disabled={drawing}>
                        { (() => {
                            if(autoGenerating) return "Stop Auto";
                            return "Start Auto";
                        })()
                        }
                    </Button>
                    <Button id="start-drawing" variant="secondary" type="button" onClick={handleStartStopDraw}
                            disabled={autoGenerating || configuration.configObject.sideLength === null}>
                        { (() => {
                            if(drawing) return "Stop Drawing";
                            return "Draw Generation";
                        })()
                        }
                    </Button>
                </Col>
            </Row>
            <Row style={{padding: "5px"}}>
                <Col>
                    <canvas onMouseDown={handleDrawClick} style={{outlineColor: "green", outlineStyle: "solid"}} id="conway-output" width="600" height="600"></canvas>
                </Col>
            </Row>
        </Container>
    )

}

export default Conway;