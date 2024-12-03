import React, {useEffect, useState} from "react";
import {DidAggregateJsClient, didClient} from "fixers-did-domain";
import {DidRSocketClientContext} from "./DidClientContext";

interface Props {
    children: JSX.Element
}

export const DidRSocketClientProvider = ({children}: Props) => {
    const [client, setClient] = useState<DidAggregateJsClient>()
    useEffect(() => {
        didClient(io.komune.s2.client.ktor.WS, "localhost", 7000, null).then((it: DidAggregateJsClient) => {
            setClient(it)
        })
    })

    return (
        <DidRSocketClientContext.Provider value={{initialized: !!client, client}}>
            {children}
        </DidRSocketClientContext.Provider>
    );
};