package models;

import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.AbstractListModel;

/**
 * ListModel contenant des noms uniques (toujours tri� gr�ce � un TreeSet par
 * exemple).
 * L'acc�s � la liste de noms doit �tre thread safe (c'�d : plusieurs threads
 * peuvent acc�der concurrentiellement � la liste de noms sans que celle ci se
 * retrouve dans un �tat incoh�rent) : Les modifications du Set interne se font
 * toujours dans un bloc synchronized(nameSet) {...}.
 * L'ajout ou le retrait d'un �l�ment dans l'ensemble de nom est accompagn�
 * d'un fireContentsChanged sur l'ensemble des �l�ments de la liste (� cause
 * du tri implicite des �l�ments) ce qui permet au List Model de notifier
 * tout widget dans lequel serait contenu ce ListModel.
 * @see {@link javax.swing.AbstractListModel}
 */
public class NameSetListModel extends AbstractListModel<String>
{
	/**
	 * Ensemble de noms tri�s
	 */
	private SortedSet<String> nameSet;

	/**
	 * Constructeur
	 */
	public NameSetListModel()
	{
		// TODO nameSet = ...
		nameSet = new TreeSet<String>();
	}

	/**
	 * Ajout d'un �l�ment
	 * @param value la valeur � ajouter
	 * @return true si l'�l�ment � ajouter est non null et qu'il n'�tait pas
	 * d�j� pr�sent dans l'ensemble et false sinon.
	 * @warning Ne pas oublier de faire un
	 * {@link #fireContentsChanged(Object, int, int)} lorsqu'un nom est
	 * effectivement ajout� � l'ensemble des noms
	 */
	public boolean add(String value)
	{
		// TODO Replace with implementation ...
		if(value==null) {
			return false;
		}
		else {
			// si l'�l�ment est d�j� contenu, on ne fait rien, sinon on l'ajoute
			if(this.contains(value)) {
				return false;
			}
			else {
				nameSet.add(value);
				fireContentsChanged(this, 0, getSize());
				return true;
			}
		}
	}

	/**
	 * Teste si l'ensemble de noms contient le nom pass� en argument
	 * @param value le nom � rechercher
	 * @return true si l'ensemble de noms contient "value", false sinon.
	 */
	public boolean contains(String value)
	{
		// TODO Replace with implementation ...
		if(nameSet.contains(value)) {
			return true;
		}
		else {
			return false;
		}
		
	}

	/**
	 * Retrait de l'�l�ment situ� � l'index index
	 * @param index l'index de l'�l�ment � supprimer
	 * @return true si l'�l�ment a �t� supprim�, false sinon
	 * @warning Ne pas oublier de faire un
	 * {@link #fireContentsChanged(Object, int, int)} lorsqu'un nom est
	 * effectivement supprim� de l'ensemble des noms
	 */
	public boolean remove(int index)
	{
		// TODO Replace with implementation ...
		// Il faut que l'index soit valide ( >0 et <taille et que la liste ne soit pas vide)
		if(index >=0 && index < this.getSize() && this.getSize()>0) {
			String toRemove = this.getElementAt(index);
			nameSet.remove(toRemove);
			fireContentsChanged(this, 0, getSize());
			return true;
		}
		else {
			return false;
		}
		
	}

	/**
	 * Efface l'ensemble du contenu de la liste
	 * @warning ne pas oublier de faire un
	 * {@link #fireContentsChanged(Object, int, int)} lorsque le contenu est
	 * effectivement effac� (si non vide)
	 */
	public void clear()
	{
		// TODO Complete ...
		nameSet.removeAll(nameSet);
		fireContentsChanged(this, 0, getSize());
	}

	/**
	 * Nombre d'�l�ments dans le ListModel
	 * @return le nombre d'�l�ments dans le mod�le de la liste
	 * @see javax.swing.ListModel#getSize()
	 */
	@Override
	public int getSize()
	{
		// TODO Replace with implementation ...
		return nameSet.size();
	}

	/**
	 * Accesseur � l'�l�ment index�
	 * @param l'index de l'�l�ment recherch�
	 * @return la chaine de caract�re correponsdant � l'�l�ment recherch� ou
	 * bien null si celui ci n'existe pas
	 * @see javax.swing.ListModel#getElementAt(int)
	 */
	@Override
	public String getElementAt(int index)
	{
		// TODO Replace with implementation ...
		if(index >=0 && index < this.getSize() && this.getSize()>0) {
			String res="";
			Iterator<String> it = nameSet.iterator();
			int i = 0;
			while (i!=index && it.hasNext()) {
				res = it.next();
				i++;
			}
			return res;
		}
		else {
			return null;
		}
	}

	/**
	 * Repr�sentation sous forme de chaine de caract�res de la liste de
	 * noms unique et tri�s.
	 * @return une chaine de caract�res repr�setant la liste des noms uniques
	 * et tri�s
	 */
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		for (Iterator<String> it = nameSet.iterator(); it.hasNext();)
		{
			sb.append(it.next());
			if (it.hasNext())
			{
				sb.append(", ");
			}
		}
		return sb.toString();
	}
}
