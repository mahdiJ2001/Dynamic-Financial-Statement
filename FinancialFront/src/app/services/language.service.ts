import { Injectable } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { BehaviorSubject } from 'rxjs';

@Injectable({
    providedIn: 'root'
})
export class LanguageService {
    private currentLang = new BehaviorSubject<string>('fr');

    constructor(private translate: TranslateService) {
        // Langues supportées
        translate.addLangs(['fr', 'en']);

        // Langue par défaut
        translate.setDefaultLang('fr');

        // Récupérer la langue du navigateur
        const browserLang = translate.getBrowserLang();
        translate.use(browserLang?.match(/fr|en/) ? browserLang : 'fr');

        // Mettre à jour le BehaviorSubject
        this.currentLang.next(translate.currentLang);
    }

    getCurrentLang() {
        return this.currentLang.asObservable();
    }

    setLanguage(lang: string) {
        if (this.translate.getLangs().includes(lang)) {
            this.translate.use(lang);
            this.currentLang.next(lang);
        }
    }

    toggleLanguage() {
        const currentLang = this.translate.currentLang;
        const newLang = currentLang === 'fr' ? 'en' : 'fr';
        this.setLanguage(newLang);
    }
}
