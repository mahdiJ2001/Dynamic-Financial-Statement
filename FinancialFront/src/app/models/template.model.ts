// template.model.ts
export class Template {
    id: number;
    name: string;
    formStructure: string;
    dateCreation: string;

    constructor(id: number, name: string, formStructure: string, dateCreation: string) {
        this.id = id;
        this.name = name;
        this.formStructure = formStructure;
        this.dateCreation = dateCreation;
    }
}
