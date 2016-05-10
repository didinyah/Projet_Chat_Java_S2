package chat.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.logging.Logger;

import chat.Failure;
import chat.Vocabulary;
import logger.LoggerFactory;

/**
 * User Handler Classe s'occupant de r�cup�rer ce que tape l'utilisateur et de
 * l'envoyer au serveur de chat
 *
 * @author davidroussel
 */
class UserHandler implements Runnable
{
	/**
	 * Lecteur du flux d'entr�e depuis l'utilisateur
	 */
	private BufferedReader userInBR;

	/**
	 * Ecrivain vers le flux de sortie vers le serveur
	 */
	private PrintWriter serverOutPW;

	/**
	 * Etat d'ex�cution commun du UserHandler et du {@link ServerHandler}
	 */
	private Boolean commonRun;

	/**
	 * Logger utilis� pour afficher (ou pas) les messafes d'erreurs
	 */
	private Logger logger;

	/**
	 * Constructeur d'un UserHandler
	 *
	 * @param in Le flux d'entr�e de l'utilisateur pour les entr�es utilisateur
	 * @param out le flux de sortie vers le serveur
	 * @param commonRun l'�tat d'ex�cution commun du {@link UserHandler} et du
	 *            {@link ServerHandler}
	 * @param parentLogger le logger parent
	 */
	public UserHandler(InputStream in, OutputStream out, Boolean commonRun,
			Logger parentLogger)
	{
		logger = LoggerFactory.getParentLogger(getClass(), parentLogger,
				parentLogger.getLevel());

		/*
		 * Cr�ation du lecteur de flux d'entr�e de l'utilisateur : userInBR sur
		 * l'InputStream in si celui ci est non null. Sinon on quitte avec la
		 * valeur Failure.USER_INPUT_STREAM
		 */
		if (in != null)
		{
			logger.info("UserHandler: creating user input buffered reader ... ");

			/*
			 * TODO Cr�ation du BufferedReader sur un InputStreamReader à partir
			 * du flux d'entr�e en provenance de l'utilisateur
			 */
			// userInBR = TODO Complete ...
			InputStreamReader inSR = new InputStreamReader(in);
			userInBR = new BufferedReader(inSR);
		}
		else
		{
			logger.severe("UserHandler: null input stream"
					+ Failure.USER_INPUT_STREAM);
			System.exit(Failure.USER_INPUT_STREAM.toInteger());
		}

		/*
		 * Cr�ation de l'�crivain vers le flux de sortie vers le serveur :
		 * serverOutPW sur l'OutputStream out si celui ci est non null. Sinon,
		 * on quitte avec la valeur Failure.CLIENT_OUTPUT_STREAM
		 */
		if (out != null)
		{
			logger.info("UserHandler: creating server output print writer ... ");

			/*
			 * TODO Cr�ation du PrintWriter sur le flux de sortie vers le
			 * serveur (en mode autoflush)
			 */
			// serverOutPW = TODO Complete ...
			serverOutPW = new PrintWriter(out, true); // true pour l'autoflush
		}
		else
		{
			logger.severe("UserHandler: null output stream"
					+ Failure.CLIENT_OUTPUT_STREAM);
			System.exit(Failure.CLIENT_OUTPUT_STREAM.toInteger());
		}

		/*
		 * On v�rifie que le commonRun pass� en argument est non null avant de
		 * le copier dans notre commonRun. Sinon on quitte avec la valeur
		 * Failure.OTHER
		 */
		if (commonRun != null)
		{
			this.commonRun = commonRun;
		}
		else
		{
			logger.severe("ServerHandler: null common run " + Failure.OTHER);
			System.exit(Failure.OTHER.toInteger());
		}
	}

	/**
	 * Ex�ction d'un UserHandler. Écoute les entr�es en provenance de
	 * l'utilisateur et les envoie dans le flux de sortie vers le serveur
	 *
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run()
	{
		String userInput = null;

		/*
		 * Boucle principale de lecture des messages en provenance de
		 * l'utilisateur. tantque commonRun est vrai on lit une ligne depuis le
		 * userInBR dans userInput Si cette ligne est non nulle, on l'envoie
		 * dans serverOutPW
		 */
		while (commonRun.booleanValue())
		{
			/*
			 * TODO Lecture d'une ligne en provenance de l'utilisateur gr�ce
			 * au userInBR. Si une IOException intervient - Ajout d'un
			 * severe au logger - On quitte la boucle
			 */
			// userInput = TODO Complete ...
			try {
				userInput = userInBR.readLine();
			} catch (IOException e) {
				logger.severe("UserHandler: Probl�me lors de la lecture du message");
				logger.severe(e.getLocalizedMessage());
				break;
			}

			if (userInput != null)
			{
				/*
				 * TODO Envoi du texte au serveur gr�ce au serverOutPW et
				 * v�rification de l'�tat d'erreur du serverOutPW avec ajout
				 * d'un warning au logger et break si c'est le cas.
				 */
				// TODO serverOutPW...
				serverOutPW.println(userInput);
				if(serverOutPW.checkError()) {
					logger.warning("UserHandler: error while sending text to server");
					break;
				}
				/*
				 * TODO Si la commande Vocabulary.byeCmd a �t� tap�e par
				 * l'utilisateur on quitte la boucle
				 */
				if(userInput==Vocabulary.byeCmd){
					logger.warning("UserHandler: Bye command entered");
					break;
				}
			}
			else
			{
				logger.warning("UserHandler: null user input");
				break;
			}
		}

		if (commonRun.booleanValue())
		{
			logger.info("UserHandler: changing run state at the end ... ");

			synchronized (commonRun)
			{
				commonRun = Boolean.FALSE;
			}
		}
	}

	/**
	 * Fermeture des flux
	 */
	public void cleanup()
	{
		logger.info("UserHandler: closing user input stream reader ... ");
		/*
		 * fermeture du lecteur de flux d'entr�e de l'utilisateur Si une
		 * IOException intervient : - Ajout d'un severe au logger
		 */
		try
		{
			userInBR.close();
		}
		catch (IOException e)
		{
			logger.severe("UserHandler: closing server input stream reader failed");
			logger.severe(e.getLocalizedMessage());
		}

		logger.info("UserHandler: closing server output print writer ... ");
		// fermeture de l'�crivain vers le flux de sortie vers le serveur
		serverOutPW.close();
	}
}
