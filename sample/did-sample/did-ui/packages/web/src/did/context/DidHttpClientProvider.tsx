import React, {useEffect, useState} from "react";
import {DidHttpClientContext} from "./DidClientContext"
import {city, DidAggregateJsClient, didClient} from "fixers-did-domain";

interface Props {
    children: JSX.Element
}

export const DidHttpClientProvider = ({children}: Props) => {
    const [client, setClient] = useState<DidAggregateJsClient>()
    useEffect(() => {
        didClient(io.komune.s2.client.ktor.HTTP, "localhost", 8080, null).then((it: DidAggregateJsClient) => {
            setClient(it)
        })
    })

    return (
        <DidHttpClientContext.Provider value={{initialized: !!client, client}}>
            {children}
        </DidHttpClientContext.Provider>
    );
};