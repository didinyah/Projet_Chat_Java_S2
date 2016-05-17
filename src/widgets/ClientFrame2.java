package widgets;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.util.Random;
import java.util.Stack;
import java.util.Vector;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.StyleConstants;

import models.Message;
import models.NameSetListModel;
import models.Message.MessageOrder;
import chat.Vocabulary;
import examples.widgets.ListExampleFrame.ColorTextRenderer;

/**
 * Fen�tre d'affichae de la version GUI texte du client de chat.
 * @author davidroussel
 */
public class ClientFrame2 extends AbstractClientFrame
{
	/**
	 * Lecteur de flux d'entr�e. Lit les donn�es texte du {@link #inPipe} pour
	 * les afficher dans le {@link #document}
	 */
	private BufferedReader inBR;
	private ObjectInputStream ois;

	/**
	 * Le label indiquant sur quel serveur on est connect�
	 */
	protected final JLabel serverLabel;

	/**
	 * La zone du texte � envoyer
	 */
	protected final JTextField sendTextField;
	
	/**
	 * Liste des �l�ments � afficher dans la JList.
	 * Les ajouts et retraits effectu�s dans cette ListModel seront alors
	 * automatiquement transmis au JList contenant ce ListModel
	 */
	private NameSetListModel elements = new NameSetListModel();
	//private DefaultListModel<String> elements = new DefaultListModel<String>();

	/**
	 * Le mod�le de s�lection de la JList.
	 * Conserve les indices des �l�ments s�lectionn�s de {@link #elements} dans
	 * la JList qui affiche ces �l�ments.
	 */
	private ListSelectionModel selectionModel = null;
	

	/**
	 * Action � r�aliser lorsque l'on souhaite d�selctionner tous les �lements de la liste
	 */
	private final Action clearSelectionAction;

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
	
	private final FilterAction filterAction;
	private final SortDateAction sortDateAction;
	private final SortAuthorAction sortAuthorAction;
	private final SortContentAction sortContentAction;
	private final KickSelectionAction kickSelectionAction;

	/**
	 * R�f�rence � la fen�tre courante (� utiliser dans les classes internes)
	 */
	protected final JFrame thisRef;
	private JCheckBoxMenuItem filterMenuItem;
	private JCheckBoxMenuItem sortDateMenuItem;
	private JCheckBoxMenuItem sortAuthorMenuItem;
	private JCheckBoxMenuItem sortContentMenuItem;
	private JToggleButton filterButton;
	private String pseudo;
	private Vector<Message> messages = new Vector<Message>();
	private boolean activeFilter = false;

	/**
	 * Constructeur de la fen�tre
	 * @param name le nom de l'utilisateur
	 * @param host l'h�te sur lequel on est connect�
	 * @param commonRun �tat d'ex�cution des autres threads du client
	 * @param parentLogger le logger parent pour les messages
	 * @throws HeadlessException
	 */
	public ClientFrame2(String name,
	                   String host,
	                   Boolean commonRun,
	                   Logger parentLogger)
	    throws HeadlessException
	{
		super(name, host, commonRun, parentLogger);
		elements.add(name);
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
		clearSelectionAction = new ClearSelectionAction();
		filterAction = new FilterAction();
		kickSelectionAction = new KickSelectionAction();
		sortDateAction = new SortDateAction();
		sortAuthorAction = new SortAuthorAction();
		sortContentAction = new SortContentAction();


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
		quitButton.setText("");
		toolBar.add(quitButton);
		
		toolBar.add(Box.createHorizontalStrut(20));

		JButton clearSelection = new JButton(clearSelectionAction);
		clearSelection.setText("");
		clearSelection.setEnabled(false);
		toolBar.add(clearSelection);
		
		JButton kickSelection = new JButton(kickSelectionAction);
		kickSelection.setText("");
		kickSelection.setEnabled(false);
		toolBar.add(kickSelection);
		
		toolBar.add(Box.createHorizontalStrut(20));
		
		JButton clearButton = new JButton(clearAction);
		clearButton.setText("");
		toolBar.add(clearButton);
		
		filterButton = new JToggleButton(filterAction);
		filterButton.setText("");
		filterButton.setOpaque(false);
		filterButton.setEnabled(false);
		//filterButton.setContentAreaFilled(false);
		filterButton.setBorderPainted(false);
		toolBar.add(filterButton);
		
		
		/*JLabel label = new JLabel(new ImageIcon(ClientFrame2.class
	             .getResource("/icons/filled_filter-32.png")));
		label.addMouseListener(new MouseAdapter() {
			public void MousePressed(MouseEvent e) {
				ActionEvent ae = new ActionEvent(e.getSource(), e.getID(), e.paramString());
				filterAction.actionPerformed(ae);
			}
		});
		toolBar.add(label);*/

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

		JMenu actionsMenu = new JMenu("Connections");
		menuBar.add(actionsMenu);

		JMenuItem sendMenuItem = new JMenuItem(quitAction);
		actionsMenu.add(sendMenuItem);
		
		JMenu messagesMenu = new JMenu("Messages");
		menuBar.add(messagesMenu);

		JMenuItem clearMenuItem = new JMenuItem(clearAction);
		messagesMenu.add(clearMenuItem);
		
		filterMenuItem = new JCheckBoxMenuItem(filterAction);
		filterMenuItem.setEnabled(false);
		messagesMenu.add(filterMenuItem);
		
		JMenu usersMenu = new JMenu("Users");
		menuBar.add(usersMenu);

		JMenuItem clearSelectionMenuItem = new JMenuItem(clearSelectionAction);
		clearSelectionMenuItem.setEnabled(false);
		usersMenu.add(clearSelectionMenuItem);
		
		JMenuItem kickSelectionMenuItem = new JMenuItem(kickSelectionAction);
		kickSelectionMenuItem.setEnabled(false);
		usersMenu.add(kickSelectionMenuItem);
		
		JMenu submenu = new JMenu("Sort");
		submenu.setMnemonic(KeyEvent.VK_S);

		sortDateMenuItem = new JCheckBoxMenuItem(sortDateAction);
		submenu.add(sortDateMenuItem);
		
		sortAuthorMenuItem = new JCheckBoxMenuItem(sortAuthorAction);
		submenu.add(sortAuthorMenuItem);
		
		sortContentMenuItem = new JCheckBoxMenuItem(sortContentAction);
		submenu.add(sortContentMenuItem);
		
		messagesMenu.add(submenu);

		
		JPanel leftPanel = new JPanel();
		leftPanel.setPreferredSize(new Dimension(170, 10));
		getContentPane().add(leftPanel, BorderLayout.WEST);
		leftPanel.setLayout(new BorderLayout(0, 0));
		
		JScrollPane listScrollPane = new JScrollPane();
		leftPanel.add(listScrollPane, BorderLayout.CENTER);
		
		// On ajoute l'utilisateur qui vient de se co à la liste des connectés
		
		
		JList<String> list = new JList<String>(elements);
		list.setName("Elements");
		list.setBorder(UIManager.getBorder("EditorPane.border"));
		list.setCellRenderer(new ColorTextRenderer());
		list.setForeground(new Color(new Random(pseudo.hashCode()).nextInt()).darker());
		listScrollPane.setViewportView(list);

		JPopupMenu popupMenu = new JPopupMenu();
		addPopup(list, popupMenu);

		JMenuItem mntmClearSelectionAction = new JMenuItem(clearSelectionAction);
		popupMenu.add(mntmClearSelectionAction);

		JMenuItem mntmKickSelectionAction = new JMenuItem(kickSelectionAction);
		popupMenu.add(mntmKickSelectionAction);
		
		selectionModel = list.getSelectionModel();
		selectionModel.addListSelectionListener(new ListSelectionListener()
		{
			@Override
			public void valueChanged(ListSelectionEvent e)
			{
				ListSelectionModel lsm = (ListSelectionModel) e.getSource();

				int firstIndex = e.getFirstIndex();
				int lastIndex = e.getLastIndex();
				boolean isAdjusting = e.getValueIsAdjusting();
				/*
				 * isAdjusting remains true while events like drag n drop are
				 * still processed and becomes false afterwards.
				 */
				if (!isAdjusting)
				{

					if (lsm.isSelectionEmpty())
					{
						filterMenuItem.setEnabled(false);
						clearSelectionMenuItem.setEnabled(false);
						kickSelectionMenuItem.setEnabled(false);
						kickSelectionAction.setEnabled(false);
						clearSelectionAction.setEnabled(false);
						filterAction.setEnabled(false);
						kickSelection.setEnabled(false);
						clearSelection.setEnabled(false);
						filterButton.setEnabled(false);
					}
					else
					{
						filterMenuItem.setEnabled(true);
						clearSelectionMenuItem.setEnabled(true);
						kickSelectionMenuItem.setEnabled(true);
						kickSelectionAction.setEnabled(true);
						clearSelectionAction.setEnabled(true);
						filterAction.setEnabled(true);
						kickSelection.setEnabled(true);
						clearSelection.setEnabled(true);
						filterButton.setEnabled(true);
						// Find out which indexes are selected.
						int minIndex = lsm.getMinSelectionIndex();
						int maxIndex = lsm.getMaxSelectionIndex();
						for (int i = minIndex; i <= maxIndex; i++)
						{
							if (lsm.isSelectedIndex(i))
							{
								//output.append(" " + i);
							}
						}
					}
				}
				else
				{
					// Still adjusting ...
				}
			}
		});


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
	 * @param messageIn le message � afficher dans le {@link #document}
	 * @throws BadLocationException si l'�criture dans le document �choue
	 * @see {@link examples.widgets.ExampleFrame#appendToDocument(String, Color)}
	 * @see java.text.SimpleDateFormat#SimpleDateFormat(String)
	 * @see java.util.Calendar#getInstance()
	 * @see java.util.Calendar#getTime()
	 * @see javax.swing.text.StyleConstants
	 * @see javax.swing.text.StyledDocument#insertString(int, String,
	 * javax.swing.text.AttributeSet)
	 */
	protected void writeMessage(Message messageIn)
	{
		/*
		 * ajout du message "[yyyy/MM/dd HH:mm:ss] utilisateur > message" �
		 * la fin du document avec la couleur d�termin�e d'apr�s "utilisateur"
		 * (voir AbstractClientFrame2#getColorFromName)
		 */
		
		String msgContent = messageIn.getContent();
		if(msgContent.contains("kick") && msgContent.contains("granted")){
			String pseudoKick = messageIn.getContent().split("kick ")[1].split(" ")[0];
			kickCheck(pseudoKick);
			removeUserFromList(pseudoKick);
		}
		else if(msgContent.contains(" logged out")){
			removeUserFromList(msgContent.split(" logged out")[0]);
		}
		StringBuffer sb = new StringBuffer();

		sb.append(messageIn);
		sb.append(Vocabulary.newLine);

		// source et contenu du message avec la couleur du message
		String source = messageIn.getAuthor();
		if ((source != null) && (source.length() > 0))
		{
			/*
			 * Changement de couleur du texte
			 */
			StyleConstants.setForeground(documentStyle,new Color(source.hashCode()).darker());
		}
		elements.add(source);

		try {
			document.insertString(document.getLength(),
			                      sb.toString(),
			                      documentStyle);
		} catch (BadLocationException e) {
			logger.warning("ClientFrame2: clear doc: bad location");
			logger.warning(e.getLocalizedMessage());
		}

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
				logger.warning("ClientFrame2::parseName: index out of bounds");
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
				    .warning("ClientFrame2::parseContent: index out of bounds");
				return null;
			}
		}
		else
		{
			return message;
		}
	}
	
	/**
	 * Adds a popup menu to a component
	 * @param component the parent component of the popup menu
	 * @param popup the popup menu to add
	 */
	private static void addPopup(Component component, final JPopupMenu popup)
	{
		component.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				if (e.isPopupTrigger())
				{
					showMenu(e);
				}
			}

			@Override
			public void mouseReleased(MouseEvent e)
			{
				if (e.isPopupTrigger())
				{
					showMenu(e);
				}
			}

			private void showMenu(MouseEvent e)
			{
				popup.show(e.getComponent(), e.getX(), e.getY());
			}
		});
	}
	
	private class RemoveItemAction extends AbstractAction
	{
		public RemoveItemAction()
		{
			putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.META_MASK));
			putValue(SMALL_ICON, new ImageIcon(ClientFrame2.class.getResource("/icons/remove_user-16.png")));
			putValue(LARGE_ICON_KEY, new ImageIcon(ClientFrame2.class.getResource("/icons/remove_user-32.png")));
			putValue(NAME, "Remove");
			putValue(SHORT_DESCRIPTION, "Removes item from list");
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			int minIndex = selectionModel.getMinSelectionIndex();
			int maxIndex = selectionModel.getMaxSelectionIndex();
			Stack<Integer> toRemove = new Stack<Integer>();
			for (int i = minIndex; i <= maxIndex; i++)
			{
				if (selectionModel.isSelectedIndex(i))
				{
					//output.append(" " + i);
					toRemove.push(new Integer(i));
				}
			}
			while (!toRemove.isEmpty())
			{
				int index = toRemove.pop().intValue();
				elements.remove(index);
			}
		}
	}

	private class ClearSelectionAction extends AbstractAction
	{
		public ClearSelectionAction()
		{
			putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.META_MASK));
			putValue(LARGE_ICON_KEY, new ImageIcon(ClientFrame2.class.getResource("/icons/delete_database-32.png")));
			putValue(SMALL_ICON, new ImageIcon(ClientFrame2.class.getResource("/icons/delete_database-16.png")));
			putValue(NAME, "Clear selection");
			putValue(SHORT_DESCRIPTION, "Unselect selected items");
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			//output.append("Clear selection action triggered" + newline);
			selectionModel.clearSelection();
		}
	}
	
	protected class FilterAction extends AbstractAction
	{
		public FilterAction()
		{
			putValue(SMALL_ICON,
			         new ImageIcon(ClientFrame2.class
			             .getResource("/icons/filled_filter-16.png")));
			putValue(LARGE_ICON_KEY,
			         new ImageIcon(ClientFrame2.class
			             .getResource("/icons/filled_filter-32.png")));
			putValue(ACCELERATOR_KEY,
			         KeyStroke.getKeyStroke(KeyEvent.VK_L,
			                                InputEvent.META_MASK));
			putValue(NAME, "Filter");
			putValue(SHORT_DESCRIPTION, "Filter the messages");
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
			try {
				document.remove(0, document.getLength());
			} catch (BadLocationException ex) {
				logger.warning("ClientFrame2: clear doc: bad location");
				logger.warning(ex.getLocalizedMessage());
			}
			
			Consumer<Message> messagePrinter = (Message m) -> writeMessage(m);
			
			// Ici on gère le GUI pour lier les boutons et checkbox entre eux
			if(e.getSource().getClass() == JToggleButton.class){
				JToggleButton jt = (JToggleButton)e.getSource();
				if(jt.isSelected()){
					filterMenuItem.setSelected(true);
					activeFilter = true;
				}
				else{
					filterMenuItem.setSelected(false);
					activeFilter = false;
				}
				
			}
			else
			{
				JCheckBoxMenuItem jcb = (JCheckBoxMenuItem)e.getSource();
				if(jcb.isSelected()){
					filterButton.setSelected(true);
					activeFilter = true;
				}
				else{
					filterButton.setSelected(false);
					activeFilter = false;
				}
			}
			
			// Ici, on filtre les messages
			
			if(activeFilter){
				
				Predicate<Message> selectionFilter = (Message m) ->
	            {
	                if (m != null)
	                {
	                    if (m.hasAuthor())
	                    {
	                        if (selectionModel.isSelectedIndex(elements.getIndex(m.getAuthor())))
	                        {
	                            return true;
	                        }
	                    }
	                }
	                return false;
	            };
				
	            messages.stream().sorted().filter(selectionFilter).forEach(messagePrinter);
			}
			else{
				messages.stream().sorted().forEach(messagePrinter);
			}
		}
	}
	
	protected class SortDateAction extends AbstractAction
	{
		public SortDateAction()
		{
			putValue(SMALL_ICON,
			         new ImageIcon(ClientFrame2.class
			             .getResource("/icons/clock-16.png")));
			putValue(LARGE_ICON_KEY,
			         new ImageIcon(ClientFrame2.class
			             .getResource("/icons/clock-32.png")));
			putValue(ACCELERATOR_KEY,
			         KeyStroke.getKeyStroke(KeyEvent.VK_L,
			                                InputEvent.META_MASK));
			putValue(NAME, "By Date");
			putValue(SHORT_DESCRIPTION, "Sort the messages by date");
		}

		/**
		 * Op�rations r�alis�es lorsque l'action est sollicit�e
		 * @param e �v�nement � l'origine de l'action
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
		@Override
		public void actionPerformed(ActionEvent e)
		{

			JCheckBoxMenuItem jcb = (JCheckBoxMenuItem)e.getSource();
			if(jcb.isSelected()){
				Message.addOrder(MessageOrder.DATE);
			}
			else{
				Message.removeOrder(MessageOrder.DATE);
			}
			try {
				document.remove(0, document.getLength());
				Consumer<Message> messagePrinter = (Message m) -> writeMessage(m);
				messages.stream().sorted().forEach(messagePrinter);
				
			} catch (BadLocationException ex) {
				logger.warning("ClientFrame2: clear doc: bad location");
				logger.warning(ex.getLocalizedMessage());
			}
		}
	}
	
	protected class SortAuthorAction extends AbstractAction
	{
		public SortAuthorAction()
		{
			putValue(SMALL_ICON,
			         new ImageIcon(ClientFrame2.class
			             .getResource("/icons/gender_neutral_user-16.png")));
			putValue(LARGE_ICON_KEY,
			         new ImageIcon(ClientFrame2.class
			             .getResource("/icons/gender_neutral_user-32.png")));
			putValue(ACCELERATOR_KEY,
			         KeyStroke.getKeyStroke(KeyEvent.VK_L,
			                                InputEvent.META_MASK));
			putValue(NAME, "By Author");
			putValue(SHORT_DESCRIPTION, "Sort the messages by author");
		}

		/**
		 * Op�rations r�alis�es lorsque l'action est sollicit�e
		 * @param e �v�nement � l'origine de l'action
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
		@Override
		public void actionPerformed(ActionEvent e)
		{

			JCheckBoxMenuItem jcb = (JCheckBoxMenuItem)e.getSource();
			if(jcb.isSelected()){
				Message.addOrder(MessageOrder.AUTHOR);
			}
			else{
				Message.removeOrder(MessageOrder.AUTHOR);
			}
			
			try {
				document.remove(0, document.getLength());
				Consumer<Message> messagePrinter = (Message m) -> writeMessage(m);
				messages.stream().sorted().forEach(messagePrinter);
				
			} catch (BadLocationException ex) {
				logger.warning("ClientFrame2: clear doc: bad location");
				logger.warning(ex.getLocalizedMessage());
			}
		}
	}
	
	protected class SortContentAction extends AbstractAction
	{
		public SortContentAction()
		{
			putValue(SMALL_ICON,
			         new ImageIcon(ClientFrame2.class
			             .getResource("/icons/select_all-16.png")));
			putValue(LARGE_ICON_KEY,
			         new ImageIcon(ClientFrame2.class
			             .getResource("/icons/select_all-32.png")));
			putValue(ACCELERATOR_KEY,
			         KeyStroke.getKeyStroke(KeyEvent.VK_L,
			                                InputEvent.META_MASK));
			putValue(NAME, "By Content");
			putValue(SHORT_DESCRIPTION, "Sort the messages by content");
		}

		/**
		 * Op�rations r�alis�es lorsque l'action est sollicit�e
		 * @param e �v�nement � l'origine de l'action
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
		@Override
		public void actionPerformed(ActionEvent e)
		{

			JCheckBoxMenuItem jcb = (JCheckBoxMenuItem)e.getSource();
			if(jcb.isSelected()){
				Message.addOrder(MessageOrder.CONTENT);
			}
			else{
				Message.removeOrder(MessageOrder.CONTENT);
			}
			
			try {
				document.remove(0, document.getLength());
				Consumer<Message> messagePrinter = (Message m) -> writeMessage(m);
				messages.stream().sorted().forEach(messagePrinter);
				
			} catch (BadLocationException ex) {
				logger.warning("ClientFrame2: clear doc: bad location");
				logger.warning(ex.getLocalizedMessage());
			}
		}
	}
	
	protected class KickSelectionAction extends AbstractAction
	{
		public KickSelectionAction()
		{
			putValue(SMALL_ICON,
			         new ImageIcon(ClientFrame2.class
			             .getResource("/icons/remove_user-16.png")));
			putValue(LARGE_ICON_KEY,
			         new ImageIcon(ClientFrame2.class
			             .getResource("/icons/remove_user-32.png")));
			putValue(ACCELERATOR_KEY,
			         KeyStroke.getKeyStroke(KeyEvent.VK_L,
			                                InputEvent.META_MASK));
			putValue(NAME, "Kick Selected Users");
			putValue(SHORT_DESCRIPTION, "Kick the selected users");
		}

		/**
		 * Op�rations r�alis�es lorsque l'action est sollicit�e
		 * @param e �v�nement � l'origine de l'action
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
		@Override
		public void actionPerformed(ActionEvent e)
		{
			int minIndex = selectionModel.getMinSelectionIndex();
			int maxIndex = selectionModel.getMaxSelectionIndex();
			Stack<Integer> toRemove = new Stack<Integer>();
			for (int i = minIndex; i <= maxIndex; i++)
			{
				if (selectionModel.isSelectedIndex(i))
				{
					//output.append(" " + i);
					toRemove.push(new Integer(i));
				}
			}
			while (!toRemove.isEmpty())
			{
				int index = toRemove.pop().intValue();
				String username = elements.getElementAt(index);
				sendMessage(Vocabulary.kickCmd + " " + username);
				logger.warning("KICK CMD : " + Vocabulary.kickCmd + " " + username);
			}
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
			         new ImageIcon(ClientFrame2.class
			             .getResource("/icons/erase2-16.png")));
			putValue(LARGE_ICON_KEY,
			         new ImageIcon(ClientFrame2.class
			             .getResource("/icons/erase2-32.png")));
			putValue(ACCELERATOR_KEY,
			         KeyStroke.getKeyStroke(KeyEvent.VK_L,
			                                InputEvent.META_MASK));
			putValue(NAME, "Clear Messages");
			putValue(SHORT_DESCRIPTION, "Clear all the messages");
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
				messages.clear();
			}
			catch (BadLocationException ex)
			{
				logger.warning("ClientFrame2: clear doc: bad location");
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
			         new ImageIcon(ClientFrame2.class
			             .getResource("/icons/sent-16.png")));
			putValue(LARGE_ICON_KEY,
			         new ImageIcon(ClientFrame2.class
			             .getResource("/icons/sent-32.png")));
			putValue(ACCELERATOR_KEY,
			         KeyStroke.getKeyStroke(KeyEvent.VK_S,
			                                InputEvent.META_MASK));
			//putValue(NAME, "");
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
			         new ImageIcon(ClientFrame2.class
			             .getResource("/icons/disconnected-16.png")));
			putValue(LARGE_ICON_KEY,
			         new ImageIcon(ClientFrame2.class
			             .getResource("/icons/disconnected-32.png")));
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
			try
			{
				Thread.sleep(1000);
			}
			catch (InterruptedException e1)
			{
				return;
			}
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
	
	public void removeUserFromList(String target){
		for(int i = 0; i<elements.getSize(); i++){
			if(elements.getElementAt(i).compareTo(target) == 0){
				elements.remove(i);
				break;
			}
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
				// peut-être enlever cette ligne
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
		//inBR = new BufferedReader(new InputStreamReader(inPipe));
		try {
			ois = new ObjectInputStream(inPipe);
		} catch (IOException e1) {
			logger.warning("ClientFrame2: I/O Error reading");
		}
		Message messageIn;
		elements.add(pseudo);

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
				//messageIn = inBR.readLine();
				messageIn = (Message) ois.readObject();
			}
			catch (IOException | ClassNotFoundException e)
			{
				logger.warning("ClientFrame2: I/O Error reading");
				break;
			}

			if (messageIn != null)
			{
				messages.add(messageIn);
				try {
					document.remove(0, document.getLength());
				} catch (BadLocationException ex) {
					logger.warning("ClientFrame2: clear doc: bad location");
					logger.warning(ex.getLocalizedMessage());
				}
				Consumer<Message> messagePrinter = (Message m) -> writeMessage(m);
				if(activeFilter){
					
					Predicate<Message> selectionFilter = (Message m) ->
		            {
		                if (m != null)
		                {
		                    if (m.hasAuthor())
		                    {
		                        if (selectionModel.isSelectedIndex(elements.getIndex(m.getAuthor())))
		                        {
		                            return true;
		                        }
		                    }
		                }
		                return false;
		            };
					
		            messages.stream().sorted().filter(selectionFilter).forEach(messagePrinter);
				}
				else{
					messages.stream().sorted().forEach(messagePrinter);
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
			    .info("ClientFrame2::cleanup: changing run state at the end ... ");
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
		logger.info("ClientFrame2::cleanup: closing input buffered reader ... ");
		try
		{
			//inBR.close();
			ois.close();
		}
		catch (IOException e)
		{
			logger.warning("ClientFrame2::cleanup: failed to close input reader"
			    + e.getLocalizedMessage());
		}

		super.cleanup();
	}
}