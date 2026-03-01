package fr.dominatuin.dwcompany;

import java.util.*;

/**
 * Statistics and analytics for companies.
 * Provides ranking, comparisons, and insights.
 */
public class CompanyStats {

    private final CompanyManager companyManager;

    public CompanyStats(CompanyManager companyManager) {
        this.companyManager = companyManager;
    }

    /**
     * Gets top companies by total earnings.
     */
    public List<Company> getTopByEarnings(int limit) {
        return companyManager.getAllCompanies().stream()
            .sorted((c1, c2) -> Double.compare(c2.getTotalMoneyEarned(), c1.getTotalMoneyEarned()))
            .limit(limit)
            .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Gets top companies by member count.
     */
    public List<Company> getTopByMembers(int limit) {
        return companyManager.getAllCompanies().stream()
            .sorted((c1, c2) -> Integer.compare(c2.getMemberCount(), c1.getMemberCount()))
            .limit(limit)
            .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Gets company rank by earnings.
     */
    public int getRankByEarnings(String companyName) {
        List<Company> sorted = getTopByEarnings(Integer.MAX_VALUE);
        for (int i = 0; i < sorted.size(); i++) {
            if (sorted.get(i).getName().equals(companyName)) {
                return i + 1;
            }
        }
        return -1;
    }

    /**
     * Gets total network value (company + subsidiaries).
     */
    public double getNetworkValue(Company company) {
        double total = company.getBalance();
        for (String subName : company.getSubsidiaries()) {
            Company sub = companyManager.getCompany(subName);
            if (sub != null) {
                total += sub.getBalance();
            }
        }
        return total;
    }

    /**
     * Gets total network earnings (company + subsidiaries).
     */
    public double getNetworkEarnings(Company company) {
        double total = company.getTotalMoneyEarned();
        for (String subName : company.getSubsidiaries()) {
            Company sub = companyManager.getCompany(subName);
            if (sub != null) {
                total += sub.getTotalMoneyEarned();
            }
        }
        return total;
    }

    /**
     * Gets total network members (company + subsidiaries).
     */
    public int getNetworkMembers(Company company) {
        int total = company.getMemberCount();
        for (String subName : company.getSubsidiaries()) {
            Company sub = companyManager.getCompany(subName);
            if (sub != null) {
                total += sub.getMemberCount();
            }
        }
        return total;
    }

    /**
     * Gets average member earnings.
     */
    public double getAverageMemberEarnings(Company company) {
        if (company.getMemberCount() == 0) return 0;
        return company.getTotalMoneyEarned() / company.getMemberCount();
    }

    /**
     * Gets company growth rate (earnings per day estimate).
     */
    public String getGrowthRate(Company company) {
        double earnings = company.getTotalMoneyEarned();
        if (earnings < 1000) return "§7Startup";
        if (earnings < 50000) return "§aGrowing";
        if (earnings < 250000) return "§eEstablished";
        if (earnings < 1000000) return "§6Thriving";
        return "§c§lDominant";
    }

    /**
     * Checks if company is in top 10.
     */
    public boolean isTopCompany(String companyName) {
        return getRankByEarnings(companyName) <= 10;
    }

    /**
     * Gets company performance score (0-100).
     */
    public int getPerformanceScore(Company company) {
        int score = 0;
        
        // Level contribution (0-30 points)
        score += company.getLevel() * 4;
        
        // Member count (0-20 points)
        score += (company.getMemberCount() * 2);
        
        // Balance (0-25 points)
        if (company.getBalance() > 100000) score += 25;
        else if (company.getBalance() > 50000) score += 20;
        else if (company.getBalance() > 10000) score += 15;
        else if (company.getBalance() > 1000) score += 10;
        
        // Subsidiaries (0-15 points)
        score += company.getSubsidiaries().size() * 3;
        
        // International status (0-10 points)
        if (company.isInternational()) score += 10;
        
        return Math.min(100, score);
    }

    /**
     * Gets all statistics for a company.
     */
    public Map<String, Object> getFullStats(Company company) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("rank", getRankByEarnings(company.getName()));
        stats.put("networkValue", getNetworkValue(company));
        stats.put("networkEarnings", getNetworkEarnings(company));
        stats.put("networkMembers", getNetworkMembers(company));
        stats.put("avgMemberEarnings", getAverageMemberEarnings(company));
        stats.put("growthRate", getGrowthRate(company));
        stats.put("performanceScore", getPerformanceScore(company));
        stats.put("isTop10", isTopCompany(company.getName()));
        return stats;
    }
}
