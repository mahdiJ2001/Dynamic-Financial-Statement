export interface ActivityLogDTO {
    id: number;
    user: {
        id: number;
        username: string;
        email: string;
    };
    actionType: string;
    description: string;
    timestamp: string;
}
