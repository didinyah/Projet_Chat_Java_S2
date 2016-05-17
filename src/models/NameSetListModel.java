package models;

import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.*;

/**
 * ListModel contenant des noms uniques (toujours trié grâce à un TreeSet par
 * exemple).
 * L'accès à la liste de noms doit être thread safe (c'àd : plusieurs threads
 * peuvent accéder concurrentiellement à la liste de noms sans que celle ci se
 * retrouve dans un état incohérent) : Les modifications du Set interne se font
 * toujours dans un bloc synchronized(nameSet) {...}.
 * L'ajout ou le retrait d'un élément dans l'ensemble de nom est accompagné
 * d'un fireContentsChanged sur l'ensemble des éléments de la liste (à cause
 * du tri implicite des éléments) ce qui permet au List Model de notifier
 * tout widget dans lequel serait contenu ce ListModel.
 * @see {@link javax.swing.AbstractListModel}
 */
public class NameSetListModel extends AbstractListModel<String>
{
	/**
	 * Ensemble de noms triés
	 */
	private SortedSet<String> nameSet;

	/**
	 * Constructeur
	 */
	public NameSetListModel()
	{
		nameSet = new TreeSet<>();
	}

	/**
	 * Ajout d'un élément
	 * @param value la valeur à ajouter
	 * @return true si l'élément à ajouter est non null et qu'il n'était pas
	 * déjà présent dans l'ensemble et false sinon.
	 * @warning Ne pas oublier de faire un
	 * {@link #fireContentsChanged(Object, int, int)} lorsqu'un nom est
	 * effectivement ajouté à l'ensemble des noms
	 */
	public boolean add(String value)
	{
		if(value != null) {
				nameSet.add(value);
				fireContentsChanged(this, 0, nameSet.size()-1);
				return true;
		}

		return false;
	}

	/**
	 * Teste si l'ensemble de noms contient le nom passé en argument
	 * @param value le nom à rechercher
	 * @return true si l'ensemble de noms contient "value", false sinon.
	 */
	public boolean contains(String value)
	{
		return(nameSet.contains(value));
	}

	/**
	 * Retrait de l'élément situé à l'index index
	 * @param index l'index de l'élément à supprimer
	 * @return true si l'élément a été supprimé, false sinon
	 * @warning Ne pas oublier de faire un
	 * {@link #fireContentsChanged(Object, int, int)} lorsqu'un nom est
	 * effectivement supprimé de l'ensemble des noms
	 */
	public boolean remove(int index)
	{
		int count = 0;
		Iterator it = nameSet.iterator();
		
		while(it.hasNext()){
			it.next();
			if(count == index){
				it.remove();
				fireContentsChanged(this, 0, nameSet.size()-1);
				return true;
			}
			count++;
		}

		return false;
	}

	/**
	 * Efface l'ensemble du contenu de la liste
	 * @warning ne pas oublier de faire un
	 * {@link #fireContentsChanged(Object, int, int)} lorsque le contenu est
	 * effectivement effacé (si non vide)
	 */
	public void clear()
	{
		nameSet.removeAll(nameSet);
		fireContentsChanged(this, 0, nameSet.size()-1);
	}

	/**
	 * Nombre d'éléments dans le ListModel
	 * @return le nombre d'éléments dans le modèle de la liste
	 * @see javax.swing.ListModel#getSize()
	 */
	@Override
	public int getSize()
	{
		return nameSet.size();
	}

	/**
	 * Accesseur à l'élément indexé
	 * @param index l'index de l'élément recherché
	 * @return la chaine de caractère correponsdant à l'élément recherché ou
	 * bien null si celui ci n'existe pas
	 * @see javax.swing.ListModel#getElementAt(int)
	 */
	@Override
	public String getElementAt(int index)
	{
		String res = null;
		
		if(index >= 0 && index < this.getSize() && this.getSize() > 0){
			int count = 0;
			Iterator it = nameSet.iterator();
			while(it.hasNext()){
				String next = (String) it.next();
				if(count == index){
					res = next;
				}
				count++;
			}
		}
		return res;
	}

	/**
	 * Représentation sous forme de chaine de caractères de la liste de
	 * noms unique et triés.
	 * @return une chaine de caractères représetant la liste des noms uniques
	 * et triés
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

	//Retourner l'index d'un élément du set
	public int getIndex(String s)
	{
		Iterator i = nameSet.iterator();
		int count = 0;

		while(i.hasNext()){
			if(i.next().equals(s)){
				break;
			}
			count++;
		}

		return count-1;
	}
}