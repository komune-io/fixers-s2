import React from "react";
import {DIDFunctionClient} from "s2-did-domain";

export type DidClientContextType = {
    client?: DIDFunctionClient;
    initialized: Boolean;
};

export const DidHttpClientContext = React.createContext<DidClientContextType>({initialized: false});
