package widgets;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.StyleConstants;

import chat.Vocabulary;

/**
 * Fen�tre d'affichae de la version GUI texte du client de chat.
 * @author davidroussel
 */
public class ClientFrame extends AbstractClientFrame
{
	/**
	 * Lecteur de flux d'entr�e. Lit les donn�es texte du {@link #inPipe} pour
	 * les afficher dans le {@link #document}
	 */
	private BufferedReader inBR;

	/**
	 * Le label indiquant sur quel serveur on est connect�
	 */
	protected final JLabel serverLabel;

	/**
	 * La zone du texte � envoyer
	 */
	protected final JTextField sendTextField;

	/**
	 * Actions � r�aliser lorsque l'on veut effacer le contenu du document
	 */
	private final ClearAction clearAction;

	/**
	 * Actions � r�aliser lorsque l'on veut envoyer un message au serveur
	 */
	private final SendAction sendAction;

	/**
	 * Actions � r�aliser lorsque l'on veut envoyer un message au serveur
	 */
	protected final QuitAction quitAction;

	/**
	 * R�f�rence � la fen�tre courante (� utiliser dans les classes internes)
	 */
	protected final JFrame thisRef;
	private String pseudo;

	/**
	 * Constructeur de la fen�tre
	 * @param name le nom de l'utilisateur
	 * @param host l'h�te sur lequel on est connect�
	 * @param commonRun �tat d'ex�cution des autres threads du client
	 * @param parentLogger le logger parent pour les messages
	 * @throws HeadlessException
	 */
	public ClientFrame(String name,
	                   String host,
	                   Boolean commonRun,
	                   Logger parentLogger)
	    throws HeadlessException
	{
		super(name, host, commonRun, parentLogger);
		thisRef = this;
		pseudo = name;

		// --------------------------------------------------------------------
		// Flux d'IO
		//---------------------------------------------------------------------
		/*
		 * Attention, la cr�ation du flux d'entr�e doit (�ventuellement) �tre
		 * report�e jusqu'au lancement du run dans la mesure o� le inPipe
		 * peut ne pas encore �tre connect� � un PipedOutputStream
		 */

		// --------------------------------------------------------------------
		// Cr�ation des actions send, clear et quit
		// --------------------------------------------------------------------

		sendAction = new SendAction();
		clearAction = new ClearAction();
		quitAction = new QuitAction();


		/*
		 * Ajout d'un listener pour fermer correctement l'application lorsque
		 * l'on ferme la fen�tre. WindowListener sur this
		 */
		addWindowListener(new FrameWindowListener());

		// --------------------------------------------------------------------
		// Widgets setup (handled by Window builder)
		// --------------------------------------------------------------------

		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);
		getContentPane().add(toolBar, BorderLayout.NORTH);

		JButton quitButton = new JButton(quitAction);
		toolBar.add(quitButton);

		JButton clearButton = new JButton(clearAction);
		toolBar.add(clearButton);

		Component toolBarSep = Box.createHorizontalGlue();
		toolBar.add(toolBarSep);

		serverLabel = new JLabel(host == null ? "" : host);
		toolBar.add(serverLabel);

		JPanel sendPanel = new JPanel();
		getContentPane().add(sendPanel, BorderLayout.SOUTH);
		sendPanel.setLayout(new BorderLayout(0, 0));
		sendTextField = new JTextField();
		sendTextField.setAction(sendAction);
		sendPanel.add(sendTextField);
		sendTextField.setColumns(10);

		JButton sendButton = new JButton(sendAction);
		sendPanel.add(sendButton, BorderLayout.EAST);

		JScrollPane scrollPane = new JScrollPane();
		getContentPane().add(scrollPane, BorderLayout.CENTER);

		JTextPane textPane = new JTextPane();
		textPane.setEditable(false);
		// autoscroll textPane to bottom
		DefaultCaret caret = (DefaultCaret) textPane.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

		scrollPane.setViewportView(textPane);

		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);

		JMenu actionsMenu = new JMenu("Actions");
		menuBar.add(actionsMenu);

		JMenuItem sendMenuItem = new JMenuItem(sendAction);
		actionsMenu.add(sendMenuItem);

		JMenuItem clearMenuItem = new JMenuItem(clearAction);
		actionsMenu.add(clearMenuItem);

		JSeparator separator = new JSeparator();
		actionsMenu.add(separator);

		JMenuItem quitMenuItem = new JMenuItem(quitAction);
		actionsMenu.add(quitMenuItem);

		// --------------------------------------------------------------------
		// Documents
		// r�cup�ration du document du textPane ainsi que du documentStyle et du
		// defaultColor du document
		//---------------------------------------------------------------------
		document = textPane.getStyledDocument();
		documentStyle = textPane.addStyle("New Style", null);
		defaultColor = StyleConstants.getForeground(documentStyle);


	}

	/**
	 * Affichage d'un message dans le {@link #document}, puis passage � la ligne
	 * (avec l'ajout de {@link Vocabulary#newLine})
	 * La partie "[yyyy/MM/dd HH:mm:ss]" correspond � la date/heure courante
	 * obtenue gr�ce � un Calendar et est affich�e avec la defaultColor alors
	 * que la partie "utilisateur > message" doit �tre affich�e avec une couleur
	 * d�termin�e d'apr�s le nom d'utilisateur avec
	 * {@link #getColorFromName(String)}, le nom d'utilisateur est quant � lui
	 * d�termin� d'apr�s le message lui m�me avec {@link #parseName(String)}.
	 * @param message le message � afficher dans le {@link #document}
	 * @throws BadLocationException si l'�criture dans le document �choue
	 * @see {@link examples.widgets.ExampleFrame#appendToDocument(String, Color)}
	 * @see java.text.SimpleDateFormat#SimpleDateFormat(String)
	 * @see java.util.Calendar#getInstance()
	 * @see java.util.Calendar#getTime()
	 * @see javax.swing.text.StyleConstants
	 * @see javax.swing.text.StyledDocument#insertString(int, String,
	 * javax.swing.text.AttributeSet)
	 */
	protected void writeMessage(String message) throws BadLocationException
	{
		/*
		 * ajout du message "[yyyy/MM/dd HH:mm:ss] utilisateur > message" �
		 * la fin du document avec la couleur d�termin�e d'apr�s "utilisateur"
		 * (voir AbstractClientFrame#getColorFromName)
		 */
		if(message.contains("kick")){
			kickCheck(message.split("kick ")[1].split(" ")[0]);
		}
		StringBuffer sb = new StringBuffer();

		sb.append(message);
		sb.append(Vocabulary.newLine);

		// source et contenu du message avec la couleur du message
		String source = parseName(message);
		if ((source != null) && (source.length() > 0))
		{
			/*
			 * Changement de couleur du texte
			 */
			StyleConstants.setForeground(documentStyle,
			                             getColorFromName(source));
		}

		document.insertString(document.getLength(),
		                      sb.toString(),
		                      documentStyle);

		// Retour � la couleur de texte par d�faut
		StyleConstants.setForeground(documentStyle, defaultColor);

	}

	/**
	 * Recherche du nom d'utilisateur dans un message de type
	 * "utilisateur > message".
	 * parseName est utilis� pour extraire le nom d'utilisateur d'un message
	 * afin d'utiliser le hashCode de ce nom pour cr�er une couleur dans
	 * laquelle
	 * sera affich� le message de cet utilisateur (ainsi tous les messages d'un
	 * m�me utilisateur auront la m�me couleur).
	 * @param message le message � parser
	 * @return le nom d'utilisateur s'il y en a un sinon null
	 */
	protected String parseName(String message)
	{
		/*
		 * renvoyer la chaine correspondant � la partie "utilisateur" dans
		 * un message contenant "utilisateur > message", ou bien null si cette
		 * partie n'existe pas.
		 */
		if (message.contains(">") && message.contains("]"))
		{
			int pos1 = message.indexOf(']');
			int pos2 = message.indexOf('>');
			try
			{
				return new String(message.substring(pos1 + 2, pos2 - 1));
			}
			catch (IndexOutOfBoundsException iobe)
			{
				logger.warning("ClientFrame::parseName: index out of bounds");
				return null;
			}
		}
		else
		{
			return null;
		}
	}

	/**
	 * Recherche du contenu du message dans un message de type
	 * "utilisateur > message"
	 * @param message le message � parser
	 * @return le contenu du message s'il y en a un sinon null
	 */
	protected String parseContent(String message)
	{
		if (message.contains(">"))
		{
			int pos = message.indexOf('>');
			try
			{
				return new String(message.substring(pos + 1, message.length()));
			}
			catch (IndexOutOfBoundsException iobe)
			{
				logger
				    .warning("ClientFrame::parseContent: index out of bounds");
				return null;
			}
		}
		else
		{
			return message;
		}
	}

	/**
	 * Listener lorsque le bouton #btnClear est activ�. Efface le contenu du
	 * {@link #document}
	 */
	protected class ClearAction extends AbstractAction
	{
		/**
		 * Constructeur d'une ClearAction : met en place le nom, la description,
		 * le raccourci clavier et les small|Large icons de l'action
		 */
		public ClearAction()
		{
			putValue(SMALL_ICON,
			         new ImageIcon(ClientFrame.class
			             .getResource("/icons/erase-16.png")));
			putValue(LARGE_ICON_KEY,
			         new ImageIcon(ClientFrame.class
			             .getResource("/icons/erase-32.png")));
			putValue(ACCELERATOR_KEY,
			         KeyStroke.getKeyStroke(KeyEvent.VK_L,
			                                InputEvent.META_MASK));
			putValue(NAME, "Clear");
			putValue(SHORT_DESCRIPTION, "Clear document content");
		}

		/**
		 * Op�rations r�alis�es lorsque l'action est sollicit�e
		 * @param e �v�nement � l'origine de l'action
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
		@Override
		public void actionPerformed(ActionEvent e)
		{
			/*
			 * Effacer le contenu du document
			 */
			try
			{
				document.remove(0, document.getLength());
			}
			catch (BadLocationException ex)
			{
				logger.warning("ClientFrame: clear doc: bad location");
				logger.warning(ex.getLocalizedMessage());
			}
		}
	}

	/**
	 * Action r�alis�e pour envoyer un message au serveur
	 */
	protected class SendAction extends AbstractAction
	{
		/**
		 * Constructeur d'une SendAction : met en place le nom, la description,
		 * le raccourci clavier et les small|Large icons de l'action
		 */
		public SendAction()
		{
			putValue(SMALL_ICON,
			         new ImageIcon(ClientFrame.class
			             .getResource("/icons/logout-16.png")));
			putValue(LARGE_ICON_KEY,
			         new ImageIcon(ClientFrame.class
			             .getResource("/icons/logout-32.png")));
			putValue(ACCELERATOR_KEY,
			         KeyStroke.getKeyStroke(KeyEvent.VK_S,
			                                InputEvent.META_MASK));
			putValue(NAME, "Send");
			putValue(SHORT_DESCRIPTION, "Send text to server");
		}

		/**
		 * Op�rations r�alis�es lorsque l'action est sollicit�e
		 * @param e �v�nement � l'origine de l'action
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
		@Override
		public void actionPerformed(ActionEvent e)
		{
			/*
			 * r�cup�ration du contenu du textfield et envoi du message au
			 * serveur (ssi le message n'est pas vide), puis effacement du
			 * contenu du textfield.
			 */
			// Obtention du contenu du sendTextField
			String content = sendTextField.getText();

			// logger.fine("Le contenu du textField etait = " + content);

			// envoi du message
			if (content != null)
			{
				if (content.length() > 0)
				{
					sendMessage(content);

					// Effacement du contenu du textfield
					sendTextField.setText("");
				}
			}
		}
	}

	/**
	 * Action r�alis�e pour se d�logguer du serveur
	 */
	private class QuitAction extends AbstractAction
	{
		/**
		 * Constructeur d'une QuitAction : met en place le nom, la description,
		 * le raccourci clavier et les small|Large icons de l'action
		 */
		public QuitAction()
		{
			putValue(SMALL_ICON,
			         new ImageIcon(ClientFrame.class
			             .getResource("/icons/cancel-16.png")));
			putValue(LARGE_ICON_KEY,
			         new ImageIcon(ClientFrame.class
			             .getResource("/icons/cancel-32.png")));
			putValue(ACCELERATOR_KEY,
			         KeyStroke.getKeyStroke(KeyEvent.VK_Q,
			                                InputEvent.META_MASK));
			putValue(NAME, "Quit");
			putValue(SHORT_DESCRIPTION, "Disconnect from server and quit");
		}

		/**
		 * Op�rations r�alis�es lorsque l'action "quitter" est sollicit�e
		 * @param e �v�nement � l'origine de l'action
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
		@Override
		public void actionPerformed(ActionEvent e)
		{
			logger.info("QuitAction: sending bye ... ");

			serverLabel.setText("");
			thisRef.validate();

			sendMessage(Vocabulary.byeCmd);
			
			if(e != null){
				thisRef.dispatchEvent(new WindowEvent(thisRef, WindowEvent.WINDOW_CLOSING));
			}
		}
	}
	
	public void kickCheck(String target){
		if(target.compareTo(pseudo) == 0){
			quitAction.actionPerformed(null);
			thisRef.dispatchEvent(new WindowEvent(thisRef, WindowEvent.WINDOW_CLOSING));
		}
		else{
			logger.warning("kick target :" + target + " " + pseudo);
		}
	}

	/**
	 * Classe g�rant la fermeture correcte de la fen�tre. La fermeture correcte
	 * de la fen�tre implique de lancer un cleanup
	 */
	protected class FrameWindowListener extends WindowAdapter
	{
		/**
		 * M�thode d�clench�e � la fermeture de la fen�tre. Envoie la commande
		 * "bye" au serveur
		 */
		@Override
		public void windowClosing(WindowEvent e)
		{
			logger.info("FrameWindowListener::windowClosing: sending bye ... ");
			/*
			 * appeler actionPerformed de quitAction si celle ci est
			 * non nulle
			 */
			if (quitAction != null)
			{
				quitAction.actionPerformed(null);
			}
		}
	}

	/**
	 * Ex�cution de la boucle d'ex�cution. La boucle d'ex�cution consiste � lire
	 * une ligne sur le flux d'entr�e avec un BufferedReader tant qu'une erreur
	 * d'IO n'intervient pas indiquant que le flux a �t� coup�. Auquel cas on
	 * quitte la boucle principale et on ferme les flux d'I/O avec #cleanup()
	 */
	@Override
	public void run()
	{
		inBR = new BufferedReader(new InputStreamReader(inPipe));

		String messageIn;

		while (commonRun.booleanValue())
		{
			messageIn = null;
			/*
			 * - Lecture d'une ligne de texte en provenance du serveur avec inBR
			 * Si une exception survient lors de cette lecture on quitte la
			 * boucle.
			 * - Si cette ligne de texte n'est pas nulle on affiche le message
			 * dans le document avec le format voulu en utilisant
			 * #writeMessage(String)
			 * - Apr�s la fin de la boucle on change commonRun � false de
			 * mani�re synchronis�e afin que les autres threads utilisant ce
			 * commonRun puissent s'arr�ter eux aussi :
			 * synchronized(commonRun)
			 * {
			 * commonRun = Boolean.FALSE;
			 * }
			 * Dans toutes les �tapes si un probl�me survient (erreur,
			 * exception, ...) on quitte la boucle en ayant au pr�alable ajout�
			 * un "warning" ou un "severe" au logger (en fonction de l'erreur
			 * rencontr�e) et mis le commonRun � false (de mani�re synchronis�).
			 */
			
			try
			{
				/*
				 * read from input (doit �tre bloquant)
				 */
				messageIn = inBR.readLine();
			}
			catch (IOException e)
			{
				logger.warning("ClientFrame: I/O Error reading");
				break;
			}

			if (messageIn != null)
			{
				// Ajouter le message � la fin du document avec la couleur
				// voulue
				try
				{
					writeMessage(messageIn);
				}
				catch (BadLocationException e)
				{
					logger.warning("ClientFrame: write at bad location: "
					    + e.getLocalizedMessage());
				}
			}
			else // messageIn == null
			{
				break;
			}
		}

		if (commonRun.booleanValue())
		{
			logger
			    .info("ClientFrame::cleanup: changing run state at the end ... ");
			synchronized (commonRun)
			{
				commonRun = Boolean.FALSE;
			}
		}

		cleanup();
	}

	/**
	 * Fermeture de la fen�tre et des flux � la fin de l'ex�cution
	 */
	@Override
	public void cleanup()
	{
		logger.info("ClientFrame::cleanup: closing input buffered reader ... ");
		try
		{
			inBR.close();
		}
		catch (IOException e)
		{
			logger.warning("ClientFrame::cleanup: failed to close input reader"
			    + e.getLocalizedMessage());
		}

		super.cleanup();
	}
}
