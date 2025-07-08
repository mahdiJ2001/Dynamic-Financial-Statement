import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TemplateListService } from '../../services/template-list.service';
import * as FileSaver from 'file-saver';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import * as bootstrap from 'bootstrap';
import Swal from 'sweetalert2';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { LanguageService } from '../../services/language.service';

@Component({
  selector: 'app-report-list',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslateModule],
  templateUrl: './report-list.component.html',
  styleUrls: ['./report-list.component.css']
})
export class ReportListComponent implements OnInit {
  financialStatements: any[] = [];
  filteredStatements: any[] = [];
  page: number = 1;
  pageSize: number = 10;
  Math = Math;
  searchTerm: string = '';
  currentUserRole: string = '';
  rejectionCause: string = '';

  filterStatus: string = '';
  filterContributor: string = '';
  filterDate: string = '';

  private modalInstance: any;
  private chatModalInstance: any;
  currentLang: string = 'fr';

  // Chat modal properties
  selectedFinancialStatement: any = null;
  chatMessages: any[] = [];
  newMessage: string = '';
  isRejectionMode: boolean = false;

  constructor(private templateService: TemplateListService,
    private authService: AuthService,
    private sanitizer: DomSanitizer,
    private languageService: LanguageService,
    private translate: TranslateService
  ) {
    this.languageService.getCurrentLang().subscribe(lang => {
      this.currentLang = lang;
    });
  }

  ngOnInit(): void {
    this.loadFinancialStatements();
    this.setUserRole();
  }

  ngAfterViewInit() {
    const modalElement = document.getElementById('pdfPreviewModal');
    if (modalElement) {
      this.modalInstance = new bootstrap.Modal(modalElement);

      modalElement.addEventListener('hidden.bs.modal', () => {
        this.pdfUrl = null;
      });
    }

    // Initialize chat modal
    const chatModalElement = document.getElementById('chatModal');
    if (chatModalElement) {
      this.chatModalInstance = new bootstrap.Modal(chatModalElement);
    }
  }

  loadFinancialStatements(): void {
    this.templateService.getAllFinancialStatements().subscribe((data) => {
      this.financialStatements = data.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
      this.filteredStatements = [...this.financialStatements];
    });
  }

  applyFilter(): void {
    this.filteredStatements = this.financialStatements.filter(fs => {
      const companyName = this.getCompanyName(fs.formData).toLowerCase();
      const contributor = (fs.contributorName || '').toLowerCase();
      const statusMatch = this.filterStatus ? fs.status === this.filterStatus : true;
      const contributorMatch = this.filterContributor ? contributor.includes(this.filterContributor.toLowerCase()) : true;
      const dateMatch = this.filterDate ? new Date(fs.createdAt).toDateString() === new Date(this.filterDate).toDateString() : true;
      const searchTermMatch = this.searchTerm ? companyName.includes(this.searchTerm.toLowerCase()) : true;

      return statusMatch && contributorMatch && dateMatch && searchTermMatch;
    });
    this.page = 1;
  }

  getCompanyName(formData: string): string {
    try {
      const parsed = JSON.parse(formData);
      return parsed.companyName || this.translate.instant('reportList.table.noContributor');
    } catch {
      return this.translate.instant('reportList.table.invalidJson');
    }
  }

  getPaginatedData(): any[] {
    const startIndex = (this.page - 1) * this.pageSize;
    const endIndex = this.page * this.pageSize;
    return this.filteredStatements.slice(startIndex, endIndex);
  }

  downloadPDF(report: string, index: number): void {
    const byteCharacters = atob(report);
    const byteNumbers = Array.from(byteCharacters, (char) => char.charCodeAt(0));
    const byteArray = new Uint8Array(byteNumbers);
    const blob = new Blob([byteArray], { type: 'application/pdf' });
    FileSaver.saveAs(blob, `financial_statement_${index + 1}.pdf`);
  }

  nextPage(): void {
    if ((this.page * this.pageSize) < this.filteredStatements.length) {
      this.page++;
    }
  }

  prevPage(): void {
    if (this.page > 1) {
      this.page--;
    }
  }

  totalPages(): number {
    return Math.ceil(this.filteredStatements.length / this.pageSize);
  }

  setUserRole(): void {
    const token = this.authService.getToken();
    if (token) {
      const decodedToken = this.authService.decodeToken(token);
      this.currentUserRole = decodedToken && decodedToken.role ? decodedToken.role : '';
    }
  }

  // Validate the financial statement
  validateFinancialStatement(fs: any): void {
    console.log('Validating financial statement:', fs);
    this.templateService.updateStatus(fs.id, 'VALIDATED').subscribe(
      () => {
        fs.status = 'VALIDATED';
        Swal.fire({
          icon: 'success', title: 'Succès',
          text: this.translate.instant('reportList.messages.validationSuccess'),
          confirmButtonColor: '#28a745'
        });
      },
      (error) => {
        Swal.fire({
          icon: 'error',
          title: 'Erreur', text: this.translate.instant('reportList.messages.error'),
          confirmButtonColor: '#d33'
        });
      }
    );
  }

  // Chat Modal Methods
  openChatModal(financialStatement: any, isRejection: boolean = false): void {
    this.selectedFinancialStatement = financialStatement;
    this.isRejectionMode = isRejection;
    this.newMessage = '';

    // Load existing chat messages for this financial statement
    this.loadChatMessages(financialStatement.id);

    // Open the modal using Bootstrap
    if (this.chatModalInstance) {
      this.chatModalInstance.show();
    }
  }

  closeChatModal(): void {
    if (this.chatModalInstance) {
      this.chatModalInstance.hide();
    }

    // Reset modal state
    this.selectedFinancialStatement = null;
    this.chatMessages = [];
    this.newMessage = '';
    this.isRejectionMode = false;
  }

  loadChatMessages(financialStatementId: string): void {
    console.log('Calling loadChatMessages with ID:', financialStatementId);
    this.templateService.getChatMessages(financialStatementId).subscribe({
      next: (messages) => {
        console.log('Received messages:', messages);
        // Map the API response to match template structure
        this.chatMessages = Array.isArray(messages) ? messages.map((msg: any) => ({
          id: msg.id,
          content: msg.messageContent, // Map messageContent to content
          sender: msg.sender,
          senderRole: msg.sender && msg.sender.role ? msg.sender.role : undefined, // Extract role for easier access
          senderName: msg.sender && msg.sender.username ? msg.sender.username : undefined, // Extract username for easier access
          timestamp: msg.sentAt // Map sentAt to timestamp
        })) : [];
        console.log('Mapped messages:', this.chatMessages); // Debug the mapped structure
        setTimeout(() => this.scrollChatToBottom(), 100);
      },
      error: (error) => {
        console.error('Error loading chat messages:', error);
        this.chatMessages = [];
      }
    });
  }


  sendMessage(): void {
    if (!this.newMessage || !this.newMessage.trim() || !this.selectedFinancialStatement) {
      return;
    }

    const currentUserId = this.authService.getCurrentUserId();

    // Validate that currentUserId is not null and convert to number
    if (!currentUserId) {
      console.error('User ID is not available');
      Swal.fire({
        icon: 'error',
        title: 'Erreur',
        text: 'Utilisateur non identifié',
        confirmButtonColor: '#d33'
      });
      return;
    }

    const senderIdNumber = typeof currentUserId === 'string' ? parseInt(currentUserId) : currentUserId;

    // Convert string ID to number if needed for the API call
    const financialStatementId = typeof this.selectedFinancialStatement.id === 'string'
      ? parseInt(this.selectedFinancialStatement.id)
      : this.selectedFinancialStatement.id;

    const messageData = {
      financialStatementId: this.selectedFinancialStatement.id.toString(), // Keep as string for component compatibility
      senderId: senderIdNumber, // Now guaranteed to be a number
      senderRole: this.currentUserRole,
      content: this.newMessage.trim(),
      timestamp: new Date()
    };

    // Call your API to send the message
    this.templateService.sendChatMessage(messageData).subscribe({
      next: (response) => {
        // Create the message object that matches your chat message structure
        const newMessage = {
          id: response.id,
          financialStatementId: financialStatementId,
          senderId: senderIdNumber,
          senderName: this.authService.getCurrentUsername() || 'User', // Fixed method name
          senderRole: this.currentUserRole,
          content: this.newMessage.trim(),
          timestamp: new Date(),
          createdAt: new Date() // Add this if your message structure expects it
        };

        // Add the new message to the chat
        this.chatMessages.push(newMessage);

        this.newMessage = '';
        setTimeout(() => this.scrollChatToBottom(), 100);
      },
      error: (error) => {
        console.error('Error sending message:', error);
        Swal.fire({
          icon: 'error',
          title: 'Erreur',
          text: this.translate.instant('reportList.messages.messageError'),
          confirmButtonColor: '#d33'
        });
      }
    });
  }

  confirmRejection(): void {
    if (this.chatMessages.length === 0) {
      Swal.fire({
        icon: 'warning',
        title: 'Attention',
        text: 'Vous devez envoyer au moins un message avant de rejeter le rapport.',
        confirmButtonColor: '#f0ad4e'
      });
      return;
    }

    // Update the status to rejected
    this.templateService.updateStatus(this.selectedFinancialStatement.id, 'REJECTED').subscribe({
      next: () => {
        this.selectedFinancialStatement.status = 'REJECTED';
        this.closeChatModal();

        Swal.fire({
          icon: 'success',
          title: 'Succès',
          text: this.translate.instant('reportList.messages.rejectionSuccess'),
          confirmButtonColor: '#28a745'
        });
      },
      error: (error) => {
        console.error('Error rejecting financial statement:', error);
        Swal.fire({
          icon: 'error',
          title: 'Erreur',
          text: this.translate.instant('reportList.messages.error'),
          confirmButtonColor: '#d33'
        });
      }
    });
  }

  private scrollChatToBottom(): void {
    const chatContainer = document.querySelector('.chat-container');
    if (chatContainer) {
      chatContainer.scrollTop = chatContainer.scrollHeight;
    }
  }

  showRejectionCauseInput: boolean = false;
  resetFilters() {
    this.filterStatus = '';
    this.filterContributor = '';
    this.filterDate = '';
    this.applyFilter();
  }

  switchLanguage(lang: string): void {
    this.languageService.setLanguage(lang);
    this.currentLang = lang;
  }

  pdfUrl: SafeResourceUrl | null = null;

  openPreviewModal(base64Pdf: string): void {
    try {
      const byteCharacters = atob(base64Pdf);
      const byteNumbers = Array.from(byteCharacters, char => char.charCodeAt(0));
      const byteArray = new Uint8Array(byteNumbers);
      const blob = new Blob([byteArray], { type: 'application/pdf' });

      const objectUrl = URL.createObjectURL(blob);
      this.pdfUrl = this.sanitizer.bypassSecurityTrustResourceUrl(objectUrl);

      const modalElement = document.getElementById('pdfPreviewModal');
      if (modalElement) {
        if (!this.modalInstance) {
          this.modalInstance = new bootstrap.Modal(modalElement);
        }
        this.modalInstance.show();
      } else {
        console.error("Modal element not found");
      }
    } catch (error) {
      console.error("Erreur lors de l'ouverture du PDF :", error);
    }
  }

  closeModal() {
    if (this.modalInstance) {
      this.modalInstance.hide();
    }
  }

  handleEnterKey(event: KeyboardEvent) {
    if (event.ctrlKey) {
      this.sendMessage();
      event.preventDefault(); // Prevents adding new line when sending
    }
  }
}