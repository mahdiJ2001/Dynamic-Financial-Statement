export interface Notification {
    id: number;
    messageKey: string;
    params: { [key: string]: string };
    read: boolean;
    createdAt: string;
}
