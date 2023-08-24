/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package no.softwarecontrol.idoc.webservices.restapi;

import jakarta.persistence.EntityManager;
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
        try {
            em.getTransaction().begin();
            em.persist(entity);
            em.getTransaction().commit();
        }catch (Exception e){
            e.printStackTrace(System.out);
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
            // wait and try again...
            try {
                System.out.println("===================================================");
                System.out.println("AbstractFacadeREST.edit(): Prøver igjen på deadlock");
                System.out.println("===================================================");

                Thread.sleep(5000);
                em.getTransaction().begin();
                em.merge(entity);
                em.getTransaction().commit();
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }

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
            em.close();
        }

    }

    public List<T> findAll() {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        Query query = em.createNamedQuery(getSelectAllQuery());
        //query.setParameter("loginName", id);
        //query.setFirstResult(0);
        //query.setMaxResults(10);
        List results = query.getResultList();
        em.close();
        return results;

        //javax.persistence.criteria.CriteriaQuery cq = getEntityManager().getCriteriaBuilder().createQuery();
        //cq.select(cq.from(entityClass));
        //return getEntityManager().createQuery(cq).getResultList();
    }

    public List<T> findRange(int[] range) {
        EntityManager em = LocalEntityManagerFactory.createEntityManager();
        Query query = em.createNamedQuery(getSelectAllQuery());
        //query.setParameter("loginName", id);
        query.setFirstResult(range[0]);
        query.setMaxResults(range[1] - range[0] + 1);
        List results = query.getResultList();
        em.close();
        return results;

        /*javax.persistence.criteria.CriteriaQuery cq = getEntityManager().getCriteriaBuilder().createQuery();
        cq.select(cq.from(entityClass));
        javax.persistence.Query q = getEntityManager().createQuery(cq);
        q.setMaxResults(range[1] - range[0] + 1);
        q.setFirstResult(range[0]);
        return q.getResultList();*/
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
