import React, {useContext, useEffect, useState} from 'react';
import logo from './logo.svg';
import './App.css';
import {features} from "fixers-did-domain";
import {DidHttpClientContext} from "./did/context/DidClientContext";

function App() {
    const httpDidClient = useContext(DidHttpClientContext);

    const [httpEvent, setHttpEvent] = useState<features.DidCreatedEvent | undefined>()
    useEffect(() => {
        const cmd = new features.DidCreateCommandPayload("httpDid")
        !!httpDidClient && httpDidClient.client && httpDidClient.client.createDid(cmd).then((event: features.DidCreatedEvent) => {
            console.log("///////////////////////////")
            console.log("http: " + event.id)
            console.log("///////////////////////////")
            setHttpEvent(event)
        })
    }, [httpDidClient.initialized])

    return (
        <div className="App">
            <header className="App-header">
                <img src={logo} className="App-logo" alt="logo"/>
                {httpEvent?.id}
            </header>
        </div>
    );
}

export default App;
