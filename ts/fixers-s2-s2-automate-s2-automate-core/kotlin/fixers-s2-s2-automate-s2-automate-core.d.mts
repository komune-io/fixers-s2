declare function KtSingleton<T>(): T & (abstract new() => any);
export declare interface Command extends Message {
}
export declare interface Event extends Message {
}
export declare interface Message {
}
export declare interface Problem extends Message {
}
export declare interface Query extends Message {
}
export declare interface EnvelopeDTO<T> extends WithEnvelopeId, WithEnvelopeData<T>, Message {
    readonly id: string;
    readonly data: T;
    readonly type: string;
    readonly datacontenttype?: string | null;
    readonly specversion?: string | null;
    readonly source?: string | null;
    readonly time?: string | null;
}
export declare class Envelope<T> implements EnvelopeDTO<T> {
    constructor(id: string, data: T, type: string, datacontenttype?: string | null, specversion?: string | null, source?: string | null, time?: string | null);
    get id(): string;
    get data(): T;
    get type(): string;
    get datacontenttype(): string | null;
    get specversion(): string | null;
    get source(): string | null;
    get time(): string | null;
}
export declare namespace Envelope {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new <T>() => Envelope<T>;
    }
}
export declare interface WithEnvelopeData<T> {
    readonly data: T;
}
export declare interface WithEnvelopeId {
    readonly id: string;
}
export declare interface F2ErrorDTO extends Problem {
    readonly id?: string | null;
    readonly timestamp: string;
    readonly code: number;
    readonly requestId?: string | null;
    readonly message: string;
    readonly refusal?: string | null;
    readonly parameters: Record<string, string>;
    readonly citers: string[];
}
export declare class F2Error implements F2ErrorDTO {
    constructor(message: string, id?: string | null, timestamp?: string, code?: number, requestId?: string | null, refusal?: string | null, parameters?: Record<string, string>, citers?: string[]);
    get message(): string;
    get id(): string | null;
    get timestamp(): string;
    get code(): number;
    get requestId(): string | null;
    get refusal(): string | null;
    get parameters(): Record<string, string>;
    get citers(): string[];
    toString(): string;
}
export declare namespace F2Error {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => F2Error;
    }
}
export declare function isRefusal(error: F2ErrorDTO): boolean;
export declare function refusalCode(error: F2ErrorDTO): string;
export declare abstract class HttpRefusal {
    private constructor();
    static get INVALID_PAYLOAD(): HttpRefusal & {
        get name(): "INVALID_PAYLOAD";
        get ordinal(): 0;
    };
    static get UNAUTHENTICATED(): HttpRefusal & {
        get name(): "UNAUTHENTICATED";
        get ordinal(): 1;
    };
    static get FORBIDDEN(): HttpRefusal & {
        get name(): "FORBIDDEN";
        get ordinal(): 2;
    };
    static get NOT_FOUND(): HttpRefusal & {
        get name(): "NOT_FOUND";
        get ordinal(): 3;
    };
    static get METHOD_NOT_ALLOWED(): HttpRefusal & {
        get name(): "METHOD_NOT_ALLOWED";
        get ordinal(): 4;
    };
    static get CONFLICT(): HttpRefusal & {
        get name(): "CONFLICT";
        get ordinal(): 5;
    };
    static get PAYLOAD_TOO_LARGE(): HttpRefusal & {
        get name(): "PAYLOAD_TOO_LARGE";
        get ordinal(): 6;
    };
    static get UNSUPPORTED_MEDIA_TYPE(): HttpRefusal & {
        get name(): "UNSUPPORTED_MEDIA_TYPE";
        get ordinal(): 7;
    };
    static get UNPROCESSABLE(): HttpRefusal & {
        get name(): "UNPROCESSABLE";
        get ordinal(): 8;
    };
    static get TOO_MANY_REQUESTS(): HttpRefusal & {
        get name(): "TOO_MANY_REQUESTS";
        get ordinal(): 9;
    };
    static get UNEXPECTED(): HttpRefusal & {
        get name(): "UNEXPECTED";
        get ordinal(): 10;
    };
    static get UNAVAILABLE(): HttpRefusal & {
        get name(): "UNAVAILABLE";
        get ordinal(): 11;
    };
    static values(): [typeof HttpRefusal.INVALID_PAYLOAD, typeof HttpRefusal.UNAUTHENTICATED, typeof HttpRefusal.FORBIDDEN, typeof HttpRefusal.NOT_FOUND, typeof HttpRefusal.METHOD_NOT_ALLOWED, typeof HttpRefusal.CONFLICT, typeof HttpRefusal.PAYLOAD_TOO_LARGE, typeof HttpRefusal.UNSUPPORTED_MEDIA_TYPE, typeof HttpRefusal.UNPROCESSABLE, typeof HttpRefusal.TOO_MANY_REQUESTS, typeof HttpRefusal.UNEXPECTED, typeof HttpRefusal.UNAVAILABLE];
    static valueOf(value: string): HttpRefusal;
    get name(): "INVALID_PAYLOAD" | "UNAUTHENTICATED" | "FORBIDDEN" | "NOT_FOUND" | "METHOD_NOT_ALLOWED" | "CONFLICT" | "PAYLOAD_TOO_LARGE" | "UNSUPPORTED_MEDIA_TYPE" | "UNPROCESSABLE" | "TOO_MANY_REQUESTS" | "UNEXPECTED" | "UNAVAILABLE";
    get ordinal(): 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11;
    get status(): number;
    get refusal(): string;
}
export declare namespace HttpRefusal {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => HttpRefusal;
    }
    abstract class Companion extends KtSingleton<Companion.$metadata$.constructor>() {
        private constructor();
    }
    namespace Companion {
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace $metadata$ {
            abstract class constructor {
                get DOMAIN(): string;
                of(status: number): HttpRefusal;
                private constructor();
            }
        }
    }
}
export declare class F2Exception extends /* RuntimeException */ Error {
    constructor(error: F2ErrorDTO, cause?: Error | null);
    get error(): F2ErrorDTO;
}
export declare namespace F2Exception {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => F2Exception;
    }
    abstract class Companion extends KtSingleton<Companion.$metadata$.constructor>() {
        private constructor();
    }
    namespace Companion {
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace $metadata$ {
            abstract class constructor {
                invoke(message: string, id?: string, requestId?: string, code?: number, cause?: Error | null): F2Exception;
                private constructor();
            }
        }
    }
}
export declare interface Match<T> {
    readonly negative: boolean;
    map<R>(transform: (value: T) => R): Match<R>;
    not(): Match<T>;
    and(match: Match<T>): Match<T>;
    or(match: Match<T>): Match<T>;
}
export declare interface SortDTO {
    readonly property: string;
    readonly ascending: boolean;
    readonly nullsFirst?: boolean | null;
}
export declare interface PageDTO<out OBJECT> {
    readonly total: number;
    readonly items: OBJECT[];
}
export declare class Page<out OBJECT> implements PageDTO<OBJECT> {
    constructor(total: number, items: OBJECT[]);
    get total(): number;
    get items(): OBJECT[];
}
export declare namespace Page {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new <OBJECT>() => Page<OBJECT>;
    }
}
export declare interface PageQueryDTO extends Query {
    readonly pagination?: OffsetPaginationDTO | null;
}
export declare interface PageQueryResultDTO<out OBJECT> extends Event, PageDTO<OBJECT> {
    readonly total: number;
    readonly items: OBJECT[];
    readonly pagination?: OffsetPaginationDTO | null;
}
export declare class PageQuery implements PageQueryDTO {
    constructor(pagination?: OffsetPaginationDTO | null);
    get pagination(): OffsetPaginationDTO | null;
}
export declare namespace PageQuery {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => PageQuery;
    }
}
export declare class PageQueryResult<out OBJECT> implements PageQueryResultDTO<OBJECT> {
    constructor(pagination: OffsetPagination | null, total: number, items: OBJECT[]);
    get pagination(): OffsetPagination | null;
    get total(): number;
    get items(): OBJECT[];
}
export declare namespace PageQueryResult {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new <OBJECT>() => PageQueryResult<OBJECT>;
    }
}
export declare interface Pagination {
}
export declare interface OffsetPaginationDTO extends Pagination {
    readonly offset: number;
    readonly limit: number;
}
export declare interface PagePaginationDTO extends Pagination {
    readonly page?: number | null;
    readonly size?: number | null;
}
export declare class OffsetPagination implements OffsetPaginationDTO {
    constructor(offset: number, limit: number);
    get offset(): number;
    get limit(): number;
}
export declare namespace OffsetPagination {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => OffsetPagination;
    }
}
export declare class PagePagination implements PagePaginationDTO {
    constructor(page?: number | null, size?: number | null);
    get page(): number | null;
    get size(): number | null;
}
export declare namespace PagePagination {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => PagePagination;
    }
}
export declare interface F2Consumer<T> {
}
export declare interface F2Function<in T, out R> {
}
export declare interface F2Supplier<R> {
}
export declare interface F2SupplierSingle<R> {
}
export declare class Batch {
    constructor(size?: number, concurrency?: number);
    get size(): number;
    get concurrency(): number;
    copy(size?: number, concurrency?: number): Batch;
    toString(): string;
    hashCode(): number;
    equals(other?: any): boolean;
}
export declare namespace Batch {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => Batch;
    }
}
export declare function invokeF2Function<T, R>(fnc: F2Function<T, R>, msgs: Array<T>): Promise<Array<R>>;
export declare function invokeF2Consumer<T>(fnc: F2Consumer<T>, msgs: Array<T>): Promise<void>;
export declare function invokeF2Supplier<R>(fnc: F2Supplier<R>): Promise<Array<R>>;
export declare function invokeF2SupplierSingle<R>(fnc: F2SupplierSingle<R>): Promise<R>;
export declare interface Automate {
    getAvailableTransitions(state: S2State): Array<S2Transition>;
    isAvailableTransition(currentState: S2State, msg: Message): boolean;
    isAvailableInitTransition(command: Message): boolean;
    isFinalState(state: S2State): boolean;
    isSameState(from: S2State | null, to: S2State): boolean;
}
export type ErrorCategory = "Rejected" | "Transient" | "Indeterminate" | "Conflict";
export declare namespace ErrorCategory {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => ErrorCategory;
    }
}
export declare class S2Automate implements Automate {
    constructor(name: string, version: string | null, transitions: Array<S2Transition>);
    get name(): string;
    get version(): string | null;
    get transitions(): Array<S2Transition>;
    get withResultAsAction(): boolean;
    getAvailableTransitions(state: S2State): Array<S2Transition>;
    isAvailableTransition(currentState: S2State, msg: Message): boolean;
    isAvailableInitTransition(command: Message): boolean;
    isFinalState(state: S2State): boolean;
    isSameState(from: S2State | null, to: S2State): boolean;
}
export declare namespace S2Automate {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => S2Automate;
    }
}
export declare interface S2InitCommand extends Command {
}
export declare interface S2Command<ID extends any> extends Command, WithId<ID> {
}
export declare interface S2Error {
    readonly type: string;
    readonly description: string;
    readonly date: string;
    readonly payload: Record<string, string>;
    readonly cause?: Error | null;
}
export declare class S2ErrorBase implements S2Error {
    constructor(type: string, description: string, date: string, payload: Record<string, string>, cause?: Error | null);
    get type(): string;
    get description(): string;
    get date(): string;
    get payload(): Record<string, string>;
    get cause(): Error | null;
    toString(): string;
}
export declare namespace S2ErrorBase {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => S2ErrorBase;
    }
}
export declare interface S2Event<out STATE extends S2State, ID extends any> extends Event, WithId<ID> {
    readonly id: ID;
    readonly type: STATE;
}
export declare class S2EventSuccess<out STATE extends S2State, out COMMAND extends Command, ID extends any> implements Event {
    constructor(id: ID, type: COMMAND, from: STATE, to: STATE);
    get id(): ID;
    get type(): COMMAND;
    get from(): STATE;
    get to(): STATE;
}
export declare namespace S2EventSuccess {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new <STATE extends S2State, COMMAND extends Command, ID extends any>() => S2EventSuccess<STATE, COMMAND, ID>;
    }
}
export declare class S2EventError<out STATE extends S2State, out COMMAND extends Command, ID extends any> implements Event {
    constructor(id: ID, type: COMMAND, from: STATE, to: STATE, error: S2Error);
    get id(): ID;
    get type(): COMMAND;
    get from(): STATE;
    get to(): STATE;
    get error(): S2Error;
}
export declare namespace S2EventError {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new <STATE extends S2State, COMMAND extends Command, ID extends any>() => S2EventError<STATE, COMMAND, ID>;
    }
}
export declare interface S2Role {
}
export declare interface S2State {
    readonly position: number;
}
export declare class S2InitTransition {
    constructor(to: S2StateValue, role: S2Role, action: S2TransitionValue);
    get to(): S2StateValue;
    get role(): S2Role;
    get action(): S2TransitionValue;
}
export declare namespace S2InitTransition {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => S2InitTransition;
    }
}
export declare class S2Transition {
    constructor(from: S2StateValue | null, to: S2StateValue, role: S2RoleValue, action: S2TransitionValue, result?: S2TransitionValue | null);
    get from(): S2StateValue | null;
    get to(): S2StateValue;
    get role(): S2RoleValue;
    get action(): S2TransitionValue;
    get result(): S2TransitionValue | null;
}
export declare namespace S2Transition {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => S2Transition;
    }
}
export declare class S2TransitionValue {
    constructor(name: string);
    get name(): string;
}
export declare namespace S2TransitionValue {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => S2TransitionValue;
    }
}
export declare class S2RoleValue {
    constructor(name: string);
    get name(): string;
}
export declare namespace S2RoleValue {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => S2RoleValue;
    }
}
export declare class S2StateValue {
    constructor(name: string, position: number);
    get name(): string;
    get position(): number;
}
export declare namespace S2StateValue {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => S2StateValue;
    }
}
export declare interface WithId<ID extends any> {
    readonly id: ID;
}
export declare function isAvailableTransitionByName(_this_: S2Automate, stateName: string | null, type: string): boolean;
export declare function s2StateName(status?: any): string | null;
export declare interface WithS2Id<ID extends any> {
    s2Id(): ID;
}
export declare interface WithS2IdAndStatus<ID extends any, STATE> extends WithS2Id<ID>, WithS2State<STATE> {
}
export declare interface WithS2Iteration {
    s2Iteration(): number;
    withS2Iteration(iteration: number): WithS2Iteration;
}
export declare interface WithS2State<STATE> {
    s2State(): STATE;
}