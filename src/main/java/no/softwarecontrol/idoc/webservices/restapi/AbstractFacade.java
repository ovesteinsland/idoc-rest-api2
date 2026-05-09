/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package no.softwarecontrol.idoc.webservices.restapi;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Query;
import no.softwarecontrol.idoc.webservices.persistence.LocalEntityManagerFactory;

import java.util.List;

/**
 *
 * @author ovesteinsland
 */
public abstract class AbstractFacade<T> {

    private Class<T> entityClass;

    public AbstractFacade(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    //protected abstract EntityManager getEntityManager();
    protected abstract String getSelectAllQuery();

    public void create(T entity) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        EntityTransaction tx = null;

        try {
            tx = em.getTransaction();
            tx.begin();
            em.persist(entity);
            tx.commit();
        }catch (Exception e){
            if (tx != null && tx.isActive()) {
                tx.rollback(); // Rull tilbake endringer ved feil
            }
            e.printStackTrace(System.out);
            throw e;

        } finally {
            em.close();
        }
    }

    public void edit(T entity) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(entity);
            em.getTransaction().commit();
        }catch (Exception e){
            e.printStackTrace(System.out);
        } finally {
            em.close();
        }
    }

    public void remove(T entity) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.remove(em.merge(entity));
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }

    public T find(Object id) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        try {
            return em.find(entityClass, id);
        } finally {
            if (em != null) { // Legg til null-sjekk for sikkerhets skyld
                em.close();
            }
        }
    }

    public List<T> findAll() {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        try {
            Query query = em.createNamedQuery(getSelectAllQuery());
            return query.getResultList();
        } finally {
            if (em != null) { // Legg til null-sjekk for sikkerhets skyld
                em.close();
            }
        }
    }

    public List<T> findRange(int[] range) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        try {
            Query query = em.createNamedQuery(getSelectAllQuery());
            //query.setParameter("loginName", id);
            query.setFirstResult(range[0]);
            query.setMaxResults(range[1] - range[0] + 1);
            return query.getResultList();
        } finally {
            if (em != null) { // Legg til null-sjekk for sikkerhets skyld
                em.close();
            }
        }
    }

    public int count() {
        return 0;
        /*EntityManager em = LocalEntityManagerFactory.createEntityManager();

        javax.persistence.criteria.CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
        javax.persistence.criteria.Root<T> rt = cq.from(entityClass);
        cq.select(getEntityManager().getCriteriaBuilder().count(rt));
        javax.persistence.Query q = getEntityManager().createQuery(cq);
        Query query = em.createNamedQuery(getSelectAllQuery());
        return ((Long) query.getSingleResult()).intValue();*/
    }
    
}
